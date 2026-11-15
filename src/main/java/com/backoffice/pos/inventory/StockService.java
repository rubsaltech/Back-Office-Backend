package com.backoffice.pos.inventory;

import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.catalog.ProductVariantRepository;
import com.backoffice.pos.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The single source of truth for every stock change. Each operation, in one
 * transaction, (1) updates the variant's cached counters and (2) appends a
 * {@link StockMovement} ledger row. Nothing else in the app mutates stock, so
 * the ledger always reconciles to {@code on_hand}.
 *
 * <p>Quantity-tracked (NONE) variants move stock as a signed number. Serialized
 * (SERIAL) variants additionally track one {@link StockUnit} per IMEI; for them
 * {@code on_hand} always equals the count of units {@link UnitStatus#IN_STOCK},
 * and receive/sell/return operate on specific serials.
 */
@Service
public class StockService {

    private final ProductVariantRepository variants;
    private final StockMovementRepository movements;
    private final StockUnitRepository units;

    public StockService(ProductVariantRepository variants, StockMovementRepository movements,
                        StockUnitRepository units) {
        this.variants = variants;
        this.movements = movements;
        this.units = units;
    }

    /** Record the opening-balance ledger row for a newly created variant. */
    @Transactional
    public void recordOpening(ProductVariant v) {
        record(v, MovementType.OPENING, v.getOnHand(), "Opening balance", null);
    }

    /** Receive (purchase/restock) {@code qty} units into a quantity-tracked variant. */
    @Transactional
    public ProductVariant receive(ProductVariant v, int qty, String reason) {
        if (v.isSerialized()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This item is serial-tracked — receive it by entering its serial numbers");
        }
        requirePositive(qty);
        v.setOnHand(v.getOnHand() + qty);
        v.setTotalReceived(v.getTotalReceived() + qty);
        variants.save(v);
        record(v, MovementType.RECEIVE, qty, reason, null);
        return v;
    }

    /**
     * Receive serialized units into a SERIAL variant: one {@link StockUnit} per
     * serial. Serials must be unique within the batch and not already exist for
     * this business.
     */
    @Transactional
    public ProductVariant receiveSerials(ProductVariant v, List<String> serials, String reason) {
        if (!v.isSerialized()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This item is not serial-tracked — receive it by quantity");
        }
        List<String> clean = cleanSerials(serials);
        if (clean.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter at least one serial number");
        }
        Instant now = Instant.now();
        List<StockUnit> created = new ArrayList<>();
        for (String serial : clean) {
            if (units.existsByBusinessIdAndSerial(v.getBusinessId(), serial)) {
                throw new ApiException(HttpStatus.CONFLICT, "Serial already exists: " + serial);
            }
            StockUnit u = new StockUnit();
            u.setBusinessId(v.getBusinessId());
            u.setStoreId(v.getStoreId());
            u.setProductId(v.getProduct() != null ? v.getProduct().getId() : null);
            u.setVariantId(v.getId());
            u.setSerial(serial);
            u.setStatus(UnitStatus.IN_STOCK);
            u.setReceivedAt(now);
            created.add(u);
        }
        units.saveAll(created);
        int count = created.size();
        v.setOnHand(v.getOnHand() + count);
        v.setTotalReceived(v.getTotalReceived() + count);
        variants.save(v);
        record(v, MovementType.RECEIVE, count, reason, null);
        return v;
    }

    /** Set a quantity-tracked variant's on-hand to an exact count (stock-take). */
    @Transactional
    public ProductVariant adjust(ProductVariant v, int newOnHand, String reason) {
        if (v.isSerialized()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Serial-tracked stock cannot be bulk-adjusted — receive units or mark one defective");
        }
        if (newOnHand < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "On-hand cannot be negative");
        }
        int delta = newOnHand - v.getOnHand();
        v.setOnHand(newOnHand);
        if (delta > 0) {
            v.setTotalReceived(v.getTotalReceived() + delta);
        }
        variants.save(v);
        record(v, MovementType.ADJUST, delta, reason, null);
        return v;
    }

    /** Retire a single serialized unit (e.g. damaged/lost): removes it from stock. */
    @Transactional
    public ProductVariant retireUnit(ProductVariant v, Long unitId, String reason) {
        if (!v.isSerialized()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This item is not serial-tracked");
        }
        StockUnit u = units.findById(unitId)
                .filter(x -> x.getVariantId().equals(v.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unit not found"));
        if (u.getStatus() != UnitStatus.IN_STOCK) {
            throw new ApiException(HttpStatus.CONFLICT, "Only an in-stock unit can be marked defective");
        }
        u.setStatus(UnitStatus.DEFECTIVE);
        units.save(u);
        v.setOnHand(Math.max(0, v.getOnHand() - 1));
        variants.save(v);
        record(v, MovementType.ADJUST, -1, StringUtils.hasText(reason) ? reason : "Unit retired (" + u.getSerial() + ")", null);
        return v;
    }

    /**
     * Draw down stock for a sale and return the serialized units sold (empty for
     * a quantity-tracked variant). Blocks if there isn't enough on hand.
     */
    @Transactional
    public List<StockUnit> sell(ProductVariant v, int qty, List<Long> unitIds, Long orderId, Long orderItemId) {
        if (v.isSerialized()) {
            return sellSerial(v, unitIds, orderId, orderItemId);
        }
        requirePositive(qty);
        if (v.getOnHand() < qty) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Not enough stock for \"" + describe(v) + "\" (available: " + v.getOnHand() + ")");
        }
        v.setOnHand(v.getOnHand() - qty);
        v.setQuantitySold(v.getQuantitySold() + qty);
        variants.save(v);
        record(v, MovementType.SALE, -qty, "Sale", orderId);
        return List.of();
    }

    private List<StockUnit> sellSerial(ProductVariant v, List<Long> unitIds, Long orderId, Long orderItemId) {
        if (unitIds == null || unitIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Select the unit(s) to sell for \"" + describe(v) + "\"");
        }
        if (new HashSet<>(unitIds).size() != unitIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The same unit was selected more than once");
        }
        Map<Long, StockUnit> available = units.findByVariantIdAndStatus(v.getId(), UnitStatus.IN_STOCK)
                .stream().collect(Collectors.toMap(StockUnit::getId, Function.identity()));
        Instant now = Instant.now();
        List<StockUnit> chosen = new ArrayList<>();
        for (Long id : unitIds) {
            StockUnit u = available.get(id);
            if (u == null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "A selected unit of \"" + describe(v) + "\" is no longer available");
            }
            u.setStatus(UnitStatus.SOLD);
            u.setOrderId(orderId);
            u.setOrderItemId(orderItemId);
            u.setSoldAt(now);
            chosen.add(u);
        }
        units.saveAll(chosen);
        int count = chosen.size();
        v.setOnHand(v.getOnHand() - count);
        v.setQuantitySold(v.getQuantitySold() + count);
        variants.save(v);
        record(v, MovementType.SALE, -count, "Sale", orderId);
        return chosen;
    }

    /** Return a sale's stock to the variant (order void), by serial or by quantity. */
    @Transactional
    public void restore(ProductVariant v, int qty, Long orderId, Long orderItemId) {
        if (v.isSerialized()) {
            List<StockUnit> sold = units.findByOrderItemIdAndStatus(orderItemId, UnitStatus.SOLD);
            for (StockUnit u : sold) {
                u.setStatus(UnitStatus.IN_STOCK);
                u.setOrderId(null);
                u.setOrderItemId(null);
                u.setSoldAt(null);
            }
            units.saveAll(sold);
            int count = sold.size();
            if (count == 0) {
                return;
            }
            v.setOnHand(v.getOnHand() + count);
            v.setQuantitySold(Math.max(0, v.getQuantitySold() - count));
            variants.save(v);
            record(v, MovementType.RETURN, count, "Void return", orderId);
            return;
        }
        requirePositive(qty);
        v.setOnHand(v.getOnHand() + qty);
        v.setQuantitySold(Math.max(0, v.getQuantitySold() - qty));
        variants.save(v);
        record(v, MovementType.RETURN, qty, "Void return", orderId);
    }

    // ---- helpers ----

    private void record(ProductVariant v, MovementType type, int signedQty, String reason, Long orderId) {
        StockMovement m = new StockMovement();
        m.setBusinessId(v.getBusinessId());
        m.setStoreId(v.getStoreId());
        m.setVariantId(v.getId());
        m.setMovementType(type);
        m.setQuantity(signedQty);
        m.setBalanceAfter(v.getOnHand());
        m.setReason(reason);
        m.setOrderId(orderId);
        movements.save(m);
    }

    /** Trim, drop blanks, and reject duplicates within the batch. */
    private static List<String> cleanSerials(List<String> serials) {
        if (serials == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String s : serials) {
            if (StringUtils.hasText(s)) {
                String trimmed = s.trim();
                if (!seen.add(trimmed)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate serial in this batch: " + trimmed);
                }
            }
        }
        return new ArrayList<>(seen);
    }

    private static void requirePositive(int qty) {
        if (qty <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
    }

    private static String describe(ProductVariant v) {
        String p = v.getProduct() != null ? v.getProduct().getName() : "item";
        return v.isDefault() ? p : p + " — " + v.getVariantName();
    }
}

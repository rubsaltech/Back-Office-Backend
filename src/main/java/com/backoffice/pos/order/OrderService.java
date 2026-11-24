package com.backoffice.pos.order;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductRepository;
import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.catalog.ProductVariantRepository;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.floor.RestaurantTable;
import com.backoffice.pos.floor.TableRepository;
import com.backoffice.pos.inventory.StockService;
import com.backoffice.pos.inventory.StockUnit;
import com.backoffice.pos.order.dto.OrderDtos.OrderSummary;
import com.backoffice.pos.order.dto.OrderRequest;
import com.backoffice.pos.order.dto.OrderResponse;
import com.backoffice.pos.order.dto.ServiceBillRequest;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final long ORDER_NUMBER_BASE = 1000L;

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final TableRepository tables;
    private final PaymentDeviceRepository devices;
    private final StoreResolver storeResolver;
    private final StockService stockService;
    private final OrderItemUnitRepository orderItemUnits;

    public OrderService(OrderRepository orders, ProductRepository products, ProductVariantRepository variants,
                        TableRepository tables, PaymentDeviceRepository devices, StoreResolver storeResolver,
                        StockService stockService, OrderItemUnitRepository orderItemUnits) {
        this.orders = orders;
        this.products = products;
        this.variants = variants;
        this.tables = tables;
        this.devices = devices;
        this.storeResolver = storeResolver;
        this.stockService = stockService;
        this.orderItemUnits = orderItemUnits;
    }

    /** A pending stock drawdown recorded while building items, applied after the order is saved. */
    private record Draw(ProductVariant variant, OrderItem item, int quantity, List<Long> unitIds) {
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(OrderStatus status, Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Order> page = status != null
                ? orders.findByStoreIdAndStatus(storeId, status, pageable)
                : orders.findByStoreId(storeId, pageable);
        return PageResponse.of(page, OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        Order o = load(id);
        return OrderResponse.from(o, serialsByItem(o));
    }

    @Transactional
    public OrderResponse create(OrderRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();

        Order order = new Order();
        order.setBusinessId(businessId);
        order.setStoreId(storeId);
        order.setOrderNumber(nextOrderNumber(businessId));
        order.setType(req.type());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setGuestCount(req.guestCount() == null || req.guestCount() < 1 ? 1 : req.guestCount());
        order.setKitchenNote(req.kitchenNote());
        order.setHandlerName(safeHandlerName());

        // A table is attached whenever one is supplied (dine-in verticals only);
        // customer details are stored whenever supplied (delivery/repair/service).
        // The store's vertical decides which the POS collects — the backend just
        // records what it is given.
        if (req.tableId() != null) {
            RestaurantTable table = tables.findByIdAndStoreId(req.tableId(), storeId)
                    .orElseThrow(() -> NotFoundException.of("Table", req.tableId()));
            order.setTableId(table.getId());
            order.setTableName(table.getName());
        }
        if (StringUtils.hasText(req.customerName()) || StringUtils.hasText(req.customerAddress())
                || StringUtils.hasText(req.customerPhone())) {
            order.setCustomerName(req.customerName());
            order.setCustomerPhone(req.customerPhone());
            order.setCustomerAddress(req.customerAddress());
        }

        List<Draw> draws = buildItems(order, req, storeId);
        applyTotals(order, req);
        attachPayment(order, req, storeId);

        // Persist the order first so its id (and each item's id) can link the SALE
        // ledger rows and the sold serial snapshots, then draw down stock.
        // Insufficient stock / unavailable serials throw and roll the whole tx back.
        Order saved = orders.save(order);
        Map<Long, List<String>> serialsByItem = new java.util.HashMap<>();
        for (Draw d : draws) {
            List<StockUnit> soldUnits = stockService.sell(
                    d.variant(), d.quantity(), d.unitIds(), saved.getId(), d.item().getId());
            if (!soldUnits.isEmpty()) {
                List<String> serials = new ArrayList<>();
                for (StockUnit u : soldUnits) {
                    OrderItemUnit oiu = new OrderItemUnit();
                    oiu.setOrderItemId(d.item().getId());
                    oiu.setStockUnitId(u.getId());
                    oiu.setSerial(u.getSerial());
                    orderItemUnits.save(oiu);
                    serials.add(u.getSerial());
                }
                serialsByItem.put(d.item().getId(), serials);
            }
        }
        return OrderResponse.from(saved, serialsByItem);
    }

    /**
     * Create a POS order that bills a service/repair job: a line for the service's
     * base charge plus a line per part consumed. Stock is NOT drawn here — parts
     * were already consumed when they were added to the job. Returns the order so
     * the POS can print its receipt.
     */
    @Transactional
    public OrderResponse createServiceBill(ServiceBillRequest bill) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();

        Order order = new Order();
        order.setBusinessId(businessId);
        order.setStoreId(storeId);
        order.setOrderNumber(nextOrderNumber(businessId));
        order.setType(OrderType.SERVICE);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setGuestCount(1);
        order.setHandlerName(safeHandlerName());
        if (StringUtils.hasText(bill.customerName()) || StringUtils.hasText(bill.customerPhone())) {
            order.setCustomerName(bill.customerName());
            order.setCustomerPhone(bill.customerPhone());
        }

        int sort = 0;
        BigDecimal charge = nvl(bill.serviceCharge());
        OrderItem serviceLine = new OrderItem();
        serviceLine.setProductName(StringUtils.hasText(bill.serviceName()) ? bill.serviceName() : "Service");
        serviceLine.setQuantity(1);
        serviceLine.setUnitPrice(charge);
        serviceLine.setOriginalUnitPrice(charge);
        serviceLine.setTaxAmount(BigDecimal.ZERO);
        serviceLine.setLineTotal(charge);
        serviceLine.setSortOrder(sort++);
        order.addItem(serviceLine);

        if (bill.lines() != null) {
            for (ServiceBillRequest.BillLine line : bill.lines()) {
                if (line == null) {
                    continue;
                }
                int qty = line.quantity() < 1 ? 1 : line.quantity();
                BigDecimal unit = nvl(line.unitPrice());
                OrderItem item = new OrderItem();
                item.setProductId(line.productId());
                item.setProductName(line.productName());
                item.setVariantId(line.variantId());
                item.setVariantName(line.variantName());
                item.setQuantity(qty);
                item.setUnitPrice(unit);
                item.setOriginalUnitPrice(unit);
                item.setTaxAmount(BigDecimal.ZERO);
                item.setLineTotal(unit.multiply(BigDecimal.valueOf(qty)));
                item.setSortOrder(sort++);
                order.addItem(item);
            }
        }

        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal);
        order.setTaxTotal(BigDecimal.ZERO);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setTotal(subtotal);

        Payment payment = buildPayment(bill.paymentMethod(), bill.deviceId(), subtotal, storeId);
        if (payment != null) {
            order.addPayment(payment);
        }

        Order saved = orders.save(order);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = load(id);
        order.setStatus(status);
        Order saved = orders.save(order);
        return OrderResponse.from(saved, serialsByItem(saved));
    }

    @Transactional
    public OrderResponse voidOrder(Long id) {
        Order order = load(id);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "A completed order cannot be voided");
        }
        // Return the sold stock to inventory (only on the first void).
        if (order.getStatus() != OrderStatus.VOID) {
            restoreStock(order);
            order.setStatus(OrderStatus.VOID);
        }
        Order saved = orders.save(order);
        return OrderResponse.from(saved, serialsByItem(saved));
    }

    /** Puts an order's items back into stock (on void) via the ledger. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getVariantId() == null) {
                continue;
            }
            variants.findByIdAndStoreId(item.getVariantId(), order.getStoreId())
                    .ifPresent(v -> stockService.restore(v, item.getQuantity(), order.getId(), item.getId()));
        }
    }

    // ---- helpers ----

    private Order load(Long id) {
        return orders.findByIdAndStoreId(id, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Order", id));
    }

    /** The serial numbers sold on each order line, keyed by order-item id. */
    private Map<Long, List<String>> serialsByItem(Order order) {
        List<Long> itemIds = order.getItems().stream().map(OrderItem::getId).filter(java.util.Objects::nonNull).toList();
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        return orderItemUnits.findByOrderItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        OrderItemUnit::getOrderItemId,
                        Collectors.mapping(OrderItemUnit::getSerial, Collectors.toList())));
    }

    private long nextOrderNumber(Long businessId) {
        return orders.findTopByBusinessIdOrderByOrderNumberDesc(businessId)
                .map(o -> o.getOrderNumber() + 1)
                .orElse(ORDER_NUMBER_BASE + 1);
    }

    private List<Draw> buildItems(Order order, OrderRequest req, Long storeId) {
        List<Draw> draws = new ArrayList<>();
        int sort = 0;
        for (OrderRequest.Line line : req.items()) {
            if (line == null) {
                continue;
            }
            ProductVariant variant = resolveVariant(line, storeId);
            Product product = variant.getProduct();
            int qty = line.quantity() == null || line.quantity() < 1 ? 1 : line.quantity();

            // A serialized line sells specific units; its quantity is the number of
            // serials chosen, so the cart's quantity is ignored in favour of that.
            List<Long> unitIds = line.unitIds();
            if (variant.isSerialized()) {
                if (unitIds == null || unitIds.isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Select the unit(s) to sell for \"" + product.getName() + "\"");
                }
                qty = unitIds.size();
            }

            // Capture-at-sale labels (e.g. IMEI): one value set per unit is required.
            List<String> captureLabels = product != null ? product.getSaleCaptureLabels() : List.of();
            List<Map<String, String>> captures = line.captures();
            if (captureLabels != null && !captureLabels.isEmpty()) {
                if (captures == null || captures.isEmpty()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Enter " + String.join(", ", captureLabels) + " for \"" + product.getName() + "\"");
                }
                for (Map<String, String> unit : captures) {
                    for (String lbl : captureLabels) {
                        if (unit == null || !StringUtils.hasText(unit.get(lbl))) {
                            throw new ApiException(HttpStatus.BAD_REQUEST,
                                    "Missing " + lbl + " for a unit of \"" + product.getName() + "\"");
                        }
                    }
                }
                qty = captures.size();
            }

            OrderItem item = new OrderItem();
            item.setProductId(product != null ? product.getId() : null);
            item.setProductName(product != null ? product.getName() : variant.getVariantName());
            item.setVariantId(variant.getId());
            item.setVariantName(variant.getVariantName());
            item.setSeatNumber(line.seatNumber());
            item.setQuantity(qty);
            item.setSpecialInstructions(line.specialInstructions());
            item.setSortOrder(sort++);
            if (captureLabels != null && !captureLabels.isEmpty()) {
                item.setCaptures(captures);
            }

            // Honor a manual per-line price override when supplied; otherwise use
            // the variant's current catalog price. The catalog price is unchanged.
            BigDecimal catalogPrice = nvl(variant.getPrice());
            BigDecimal unitPrice = (line.unitPrice() != null && line.unitPrice().compareTo(BigDecimal.ZERO) >= 0)
                    ? line.unitPrice()
                    : catalogPrice;
            item.setUnitPrice(unitPrice);
            item.setOriginalUnitPrice(catalogPrice);
            // Tax is per-variant now (falls back to the product's for legacy data).
            BigDecimal tax = nvl(variant.getTaxAmount());
            item.setTaxAmount(tax.multiply(BigDecimal.valueOf(qty)));
            item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
            order.addItem(item);
            draws.add(new Draw(variant, item, qty, unitIds));
        }
        if (order.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An order must contain at least one item");
        }
        return draws;
    }

    /** Resolves the sold variant for a line: by variantId, else the product's default variant. */
    private ProductVariant resolveVariant(OrderRequest.Line line, Long storeId) {
        if (line.variantId() != null) {
            return variants.findByIdAndStoreId(line.variantId(), storeId)
                    .orElseThrow(() -> NotFoundException.of("Variant", line.variantId()));
        }
        if (line.productId() != null) {
            Product p = products.findByIdAndStoreId(line.productId(), storeId)
                    .orElseThrow(() -> NotFoundException.of("Product", line.productId()));
            ProductVariant dv = p.defaultVariant();
            if (dv == null) {
                throw new ApiException(HttpStatus.CONFLICT, "Product \"" + p.getName() + "\" has no variant to sell");
            }
            return dv;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Each order line needs a product or variant");
    }

    private void applyTotals(Order order, OrderRequest req) {
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxTotal = order.getItems().stream()
                .map(OrderItem::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountTotal = BigDecimal.ZERO;
        if (req.discountType() != null && req.discountValue() != null
                && req.discountValue().compareTo(BigDecimal.ZERO) > 0) {
            order.setDiscountType(req.discountType());
            order.setDiscountValue(req.discountValue());
            discountTotal = req.discountType() == DiscountType.PERCENTAGE
                    ? subtotal.multiply(req.discountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : req.discountValue();
            // Never discount below zero.
            if (discountTotal.compareTo(subtotal) > 0) {
                discountTotal = subtotal;
            }
        }

        order.setSubtotal(subtotal);
        order.setTaxTotal(taxTotal);
        order.setDiscountTotal(discountTotal);
        order.setTotal(subtotal.add(taxTotal).subtract(discountTotal));
    }

    private void attachPayment(Order order, OrderRequest req, Long storeId) {
        OrderRequest.Payment p = req.payment();
        if (p == null) {
            return;
        }
        Payment payment = buildPayment(p.method(), p.deviceId(), order.getTotal(), storeId);
        if (payment != null) {
            order.addPayment(payment);
        }
    }

    /** Build a Payment for a given method/device/amount, or null when no method is given. */
    private Payment buildPayment(PaymentMethod method, Long deviceId, BigDecimal amount, Long storeId) {
        if (method == null) {
            return null;
        }
        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(method == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.SUCCESSFUL);
        if (method == PaymentMethod.CARD && deviceId != null) {
            PaymentDevice device = devices.findByIdAndStoreId(deviceId, storeId)
                    .orElseThrow(() -> NotFoundException.of("Payment device", deviceId));
            payment.setDeviceId(device.getId());
            payment.setDeviceSerial(device.getSerialNumber());
        }
        return payment;
    }

    private String safeHandlerName() {
        try {
            return CurrentUser.get().displayName();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

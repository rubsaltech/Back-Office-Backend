package com.backoffice.pos.catalog;

import com.backoffice.pos.catalog.dto.InventoryResponse;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.inventory.StockMovementRepository;
import com.backoffice.pos.inventory.StockService;
import com.backoffice.pos.inventory.StockUnitRepository;
import com.backoffice.pos.inventory.dto.AdjustRequest;
import com.backoffice.pos.inventory.dto.ReceiveRequest;
import com.backoffice.pos.inventory.dto.RetireRequest;
import com.backoffice.pos.inventory.dto.StockMovementResponse;
import com.backoffice.pos.inventory.dto.StockUnitResponse;
import com.backoffice.pos.inventory.dto.UnitLookupResponse;
import com.backoffice.pos.inventory.StockUnit;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The Inventory tab operates on VARIANTS (the stockable SKUs). Stock is changed
 * only through {@link StockService}, which keeps the ledger and the cached
 * on-hand in step. The old "overwrite available_qty" endpoint is gone — stock
 * now moves via explicit receive / adjust operations that are all audited.
 */
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final ProductVariantRepository variants;
    private final StockMovementRepository movements;
    private final StockUnitRepository units;
    private final StockService stockService;
    private final StoreResolver storeResolver;

    public InventoryController(ProductVariantRepository variants, StockMovementRepository movements,
                              StockUnitRepository units, StockService stockService, StoreResolver storeResolver) {
        this.variants = variants;
        this.movements = movements;
        this.units = units;
        this.stockService = stockService;
        this.storeResolver = storeResolver;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.view')")
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean lowStock,
            @PageableDefault(size = 10, sort = "variantName") Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<ProductVariant> page;
        if (StringUtils.hasText(query)) {
            page = variants.searchByStore(storeId, query.trim(), pageable);
        } else if (Boolean.TRUE.equals(lowStock)) {
            page = variants.findLowStockByStore(storeId, pageable);
        } else {
            page = variants.findByStoreId(storeId, pageable);
        }
        return PageResponse.of(page, InventoryResponse::from);
    }

    @GetMapping("/variants/{variantId}/movements")
    @PreAuthorize("hasAuthority('inventory.view')")
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> movements(
            @PathVariable Long variantId,
            @PageableDefault(size = 25) Pageable pageable) {
        requireVariant(variantId);
        return PageResponse.of(
                movements.findByVariantIdOrderByCreatedAtDesc(variantId, pageable),
                StockMovementResponse::from);
    }

    @GetMapping("/units/lookup")
    @PreAuthorize("hasAuthority('inventory.view')")
    @Transactional(readOnly = true)
    public UnitLookupResponse lookupUnit(@RequestParam String serial) {
        Long businessId = TenantContext.requireBusinessId();
        Long storeId = storeResolver.currentStoreId();
        StockUnit u = units.findByBusinessIdAndSerial(businessId, StringUtils.trimAllWhitespace(serial))
                .filter(x -> storeId.equals(x.getStoreId()))
                .orElseThrow(() -> new NotFoundException("No unit found with serial: " + serial));
        ProductVariant v = variants.findByIdAndStoreId(u.getVariantId(), storeId).orElse(null);
        String productName = v != null && v.getProduct() != null ? v.getProduct().getName() : null;
        String variantName = v != null ? v.getVariantName() : null;
        String sku = v != null ? v.getSku() : null;
        return new UnitLookupResponse(u.getId(), u.getSerial(), u.getStatus(), u.getVariantId(),
                productName, variantName, sku, u.getReceivedAt(), u.getSoldAt(), u.getOrderId());
    }

    @GetMapping("/variants/{variantId}/units")
    @PreAuthorize("hasAuthority('inventory.view')")
    @Transactional(readOnly = true)
    public List<StockUnitResponse> listUnits(@PathVariable Long variantId) {
        requireVariant(variantId);
        return units.findByVariantIdOrderByCreatedAtAsc(variantId).stream()
                .map(StockUnitResponse::from)
                .toList();
    }

    @PostMapping("/variants/{variantId}/receive")
    @PreAuthorize("hasAuthority('inventory.edit')")
    @Transactional
    public InventoryResponse receive(@PathVariable Long variantId, @RequestBody ReceiveRequest req) {
        ProductVariant v = requireVariant(variantId);
        String reason = StringUtils.hasText(req.reason()) ? req.reason() : "Restock";
        if (v.isSerialized()) {
            stockService.receiveSerials(v, req.serials(), reason);
        } else {
            stockService.receive(v, req.quantity() == null ? 0 : req.quantity(), reason);
        }
        return InventoryResponse.from(v);
    }

    @PostMapping("/variants/{variantId}/units/{unitId}/retire")
    @PreAuthorize("hasAuthority('inventory.edit')")
    @Transactional
    public InventoryResponse retireUnit(@PathVariable Long variantId, @PathVariable Long unitId,
                                        @RequestBody(required = false) RetireRequest req) {
        ProductVariant v = requireVariant(variantId);
        stockService.retireUnit(v, unitId, req == null ? null : req.reason());
        return InventoryResponse.from(v);
    }

    @PostMapping("/variants/{variantId}/adjust")
    @PreAuthorize("hasAuthority('inventory.edit')")
    @Transactional
    public InventoryResponse adjust(@PathVariable Long variantId, @Valid @RequestBody AdjustRequest req) {
        ProductVariant v = requireVariant(variantId);
        stockService.adjust(v, req.onHand(),
                StringUtils.hasText(req.reason()) ? req.reason() : "Manual adjustment");
        return InventoryResponse.from(v);
    }

    private ProductVariant requireVariant(Long variantId) {
        return variants.findByIdAndStoreId(variantId, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Variant", variantId));
    }
}

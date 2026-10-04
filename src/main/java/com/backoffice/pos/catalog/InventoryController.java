package com.backoffice.pos.catalog;

import com.backoffice.pos.catalog.dto.InventoryAdjustRequest;
import com.backoffice.pos.catalog.dto.InventoryResponse;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.common.web.PageResponse;
import com.backoffice.pos.store.StoreResolver;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final ProductRepository products;
    private final StoreResolver storeResolver;

    public InventoryController(ProductRepository products, StoreResolver storeResolver) {
        this.products = products;
        this.storeResolver = storeResolver;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory.view')")
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        Long storeId = storeResolver.currentStoreId();
        Page<Product> page = StringUtils.hasText(query)
                ? products.findByStoreIdAndNameContainingIgnoreCase(storeId, query, pageable)
                : products.findByStoreId(storeId, pageable);
        return PageResponse.of(page, InventoryResponse::from);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('inventory.edit')")
    @Transactional
    public InventoryResponse adjust(@PathVariable Long productId, @Valid @RequestBody InventoryAdjustRequest req) {
        Product p = products.findByIdAndStoreId(productId, storeResolver.currentStoreId())
                .orElseThrow(() -> NotFoundException.of("Product", productId));
        if (req.availableQty() != null) {
            p.setAvailableQty(req.availableQty());
        }
        if (req.totalQty() != null) {
            p.setTotalQty(req.totalQty());
        }
        return InventoryResponse.from(products.save(p));
    }
}

package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.catalog.TrackingType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** A variant-centric row for the Inventory tab (stock lens over a variant). */
public record InventoryResponse(
        Long variantId,
        Long productId,
        String productName,
        String variantName,
        boolean isDefault,
        Map<String, String> attributes,
        String sku,
        BigDecimal price,
        TrackingType trackingType,
        int onHand,
        int quantitySold,
        int totalReceived,
        int lowStockThreshold
) {
    public static InventoryResponse from(ProductVariant v) {
        return new InventoryResponse(
                v.getId(),
                v.getProduct() != null ? v.getProduct().getId() : null,
                v.getProduct() != null ? v.getProduct().getName() : null,
                v.getVariantName(),
                v.isDefault(),
                v.getAttributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v.getAttributes()),
                v.getSku(), v.getPrice(), v.getTrackingType(),
                v.getOnHand(), v.getQuantitySold(), v.getTotalReceived(), v.getLowStockThreshold());
    }
}

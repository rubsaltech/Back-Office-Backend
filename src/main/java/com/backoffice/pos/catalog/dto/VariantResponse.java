package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.catalog.TrackingType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public record VariantResponse(
        Long id,
        String variantName,
        Map<String, String> attributes,
        boolean isDefault,
        String sku,
        String barcode,
        BigDecimal price,
        BigDecimal purchasedPrice,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        TrackingType trackingType,
        int onHand,
        int quantitySold,
        int totalReceived,
        int lowStockThreshold,
        CatalogStatus status
) {
    public static VariantResponse from(ProductVariant v) {
        return new VariantResponse(
                v.getId(), v.getVariantName(),
                v.getAttributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(v.getAttributes()),
                v.isDefault(), v.getSku(), v.getBarcode(), v.getPrice(), v.getPurchasedPrice(),
                v.getTaxAmount(), v.getDiscountTitle(), v.getDiscountAmount(),
                v.getTrackingType(), v.getOnHand(), v.getQuantitySold(), v.getTotalReceived(),
                v.getLowStockThreshold(), v.getStatus());
    }
}

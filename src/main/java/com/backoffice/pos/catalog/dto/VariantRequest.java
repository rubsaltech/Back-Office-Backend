package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.TrackingType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A variant in a product create/update payload. {@code id} is null for a new
 * variant. {@code openingQty} is the initial on-hand and is applied only on
 * creation — thereafter stock changes go through the inventory endpoints so the
 * ledger stays authoritative.
 */
public record VariantRequest(
        Long id,
        String variantName,
        Map<String, String> attributes,
        Boolean isDefault,
        @NotBlank String sku,
        String barcode,
        BigDecimal price,
        BigDecimal purchasedPrice,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        TrackingType trackingType,
        Integer openingQty,
        Integer lowStockThreshold,
        CatalogStatus status
) {
}

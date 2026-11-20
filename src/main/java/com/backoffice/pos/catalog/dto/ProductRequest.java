package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.label.dto.AttachedLabelRequest;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product create/update payload. Price and stock live on {@link #variants};
 * tax and discount are product-level (shared by all variants). A simple product
 * sends a single default variant.
 */
public record ProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        String barcode,
        String description,
        String imageUrl,
        Long categoryId,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        CatalogStatus status,
        Boolean hasVariants,
        List<VariantRequest> variants,
        List<AttachedLabelRequest> labels,
        List<String> saleCaptureLabels
) {
}

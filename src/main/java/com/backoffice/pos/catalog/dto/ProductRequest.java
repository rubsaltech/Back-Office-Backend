package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.label.dto.AttachedLabelRequest;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        String barcode,
        String description,
        String imageUrl,
        Long categoryId,
        BigDecimal price,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        CatalogStatus status,
        Integer availableQty,
        Integer totalQty,
        List<AttachedLabelRequest> labels
) {
}

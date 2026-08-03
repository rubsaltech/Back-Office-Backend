package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
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
        List<Group> modifierGroups
) {
    public record Group(
            String name,
            boolean required,
            int minSelect,
            int maxSelect,
            int sortOrder,
            List<Option> options
    ) {
    }

    public record Option(
            String name,
            BigDecimal priceDelta,
            boolean isDefault,
            int sortOrder
    ) {
    }
}

package com.backoffice.pos.servicecatalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record ServiceItemRequest(
        @NotBlank String name,
        String description,
        BigDecimal price,
        CatalogStatus status,
        List<LineItem> products
) {
    /** A product this service consumes. */
    public record LineItem(
            Long productId,
            Integer quantity,
            Integer sortOrder
    ) {
    }
}

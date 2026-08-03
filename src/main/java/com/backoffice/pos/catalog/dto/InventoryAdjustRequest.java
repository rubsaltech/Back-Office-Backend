package com.backoffice.pos.catalog.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record InventoryAdjustRequest(
        @PositiveOrZero Integer availableQty,
        @PositiveOrZero Integer totalQty
) {
}

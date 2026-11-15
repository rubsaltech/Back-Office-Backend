package com.backoffice.pos.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Set a variant's on-hand to an exact count (stock-take correction). */
public record AdjustRequest(
        @NotNull @PositiveOrZero Integer onHand,
        String reason
) {
}

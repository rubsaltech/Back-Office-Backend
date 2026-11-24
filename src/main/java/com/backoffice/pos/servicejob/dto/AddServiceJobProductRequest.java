package com.backoffice.pos.servicejob.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Add a product/part consumed by a service job. Stock is drawn down immediately. */
public record AddServiceJobProductRequest(
        @NotNull Long variantId,
        @NotNull @Positive Integer quantity
) {
}

package com.backoffice.pos.servicejob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Add a free-form extra charge to a service job during tracking. */
public record AddServiceJobChargeRequest(
        @NotBlank String title,
        @NotNull @Positive BigDecimal amount
) {
}

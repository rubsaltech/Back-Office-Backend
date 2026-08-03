package com.backoffice.pos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PinLoginRequest(
        @NotNull Long storeId,
        @NotBlank String pin
) {
}

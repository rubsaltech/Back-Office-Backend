package com.backoffice.pos.store.dto;

import com.backoffice.pos.store.StoreStatus;
import jakarta.validation.constraints.NotBlank;

public record StoreRequest(
        @NotBlank String name,
        boolean main,
        String address,
        Double latitude,
        Double longitude,
        StoreStatus status
) {
}

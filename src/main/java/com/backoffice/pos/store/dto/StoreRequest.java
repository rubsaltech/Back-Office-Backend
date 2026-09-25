package com.backoffice.pos.store.dto;

import com.backoffice.pos.store.StoreStatus;
import com.backoffice.pos.store.StoreType;
import jakarta.validation.constraints.NotBlank;

public record StoreRequest(
        @NotBlank String name,
        StoreType type,
        String phone,
        String email,
        boolean main,
        String address,
        Double latitude,
        Double longitude,
        StoreStatus status
) {
}

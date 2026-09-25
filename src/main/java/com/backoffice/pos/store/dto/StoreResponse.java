package com.backoffice.pos.store.dto;

import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreStatus;
import com.backoffice.pos.store.StoreType;

public record StoreResponse(
        Long id,
        String name,
        StoreType type,
        String phone,
        String email,
        boolean main,
        String address,
        Double latitude,
        Double longitude,
        StoreStatus status
) {
    public static StoreResponse from(Store s) {
        return new StoreResponse(s.getId(), s.getName(), s.getType(), s.getPhone(), s.getEmail(),
                s.isMain(), s.getAddress(), s.getLatitude(), s.getLongitude(), s.getStatus());
    }
}

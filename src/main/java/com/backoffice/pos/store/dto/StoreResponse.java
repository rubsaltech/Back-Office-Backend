package com.backoffice.pos.store.dto;

import com.backoffice.pos.store.Store;
import com.backoffice.pos.store.StoreStatus;

public record StoreResponse(
        Long id,
        String name,
        boolean main,
        String address,
        Double latitude,
        Double longitude,
        StoreStatus status
) {
    public static StoreResponse from(Store s) {
        return new StoreResponse(s.getId(), s.getName(), s.isMain(), s.getAddress(),
                s.getLatitude(), s.getLongitude(), s.getStatus());
    }
}

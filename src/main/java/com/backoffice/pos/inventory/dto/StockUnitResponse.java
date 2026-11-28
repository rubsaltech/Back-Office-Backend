package com.backoffice.pos.inventory.dto;

import com.backoffice.pos.inventory.StockUnit;
import com.backoffice.pos.inventory.UnitStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** A tracked unit of a variant (its captured per-unit values), for the Units view. */
public record StockUnitResponse(
        Long id,
        String serial,
        Map<String, String> attributes,
        UnitStatus status,
        Long orderId,
        Instant receivedAt,
        Instant soldAt
) {
    public static StockUnitResponse from(StockUnit u) {
        return new StockUnitResponse(
                u.getId(), u.getSerial(),
                u.getAttributes() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(u.getAttributes()),
                u.getStatus(), u.getOrderId(), u.getReceivedAt(), u.getSoldAt());
    }
}

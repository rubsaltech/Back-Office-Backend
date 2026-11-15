package com.backoffice.pos.inventory.dto;

import com.backoffice.pos.inventory.StockUnit;
import com.backoffice.pos.inventory.UnitStatus;

import java.time.Instant;

/** A serialized (IMEI) unit of a variant, for the Units view. */
public record StockUnitResponse(
        Long id,
        String serial,
        UnitStatus status,
        Long orderId,
        Instant receivedAt,
        Instant soldAt
) {
    public static StockUnitResponse from(StockUnit u) {
        return new StockUnitResponse(
                u.getId(), u.getSerial(), u.getStatus(),
                u.getOrderId(), u.getReceivedAt(), u.getSoldAt());
    }
}

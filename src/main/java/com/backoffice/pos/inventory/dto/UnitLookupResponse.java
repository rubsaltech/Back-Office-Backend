package com.backoffice.pos.inventory.dto;

import com.backoffice.pos.inventory.UnitStatus;

import java.time.Instant;

/** Result of looking a serialized unit up by its serial / IMEI. */
public record UnitLookupResponse(
        Long unitId,
        String serial,
        UnitStatus status,
        Long variantId,
        String productName,
        String variantName,
        String sku,
        Instant receivedAt,
        Instant soldAt,
        Long orderId
) {
}

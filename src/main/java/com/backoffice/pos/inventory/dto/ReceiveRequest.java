package com.backoffice.pos.inventory.dto;

import java.util.List;

/**
 * Receive (restock) stock into a variant. For a quantity-tracked variant, send
 * {@code quantity}. For a serialized (SERIAL) variant, send {@code serials} —
 * one IMEI / serial per unit. The controller picks the mode from the variant's
 * tracking type and validates the matching field.
 */
public record ReceiveRequest(
        Integer quantity,
        List<String> serials,
        String reason
) {
}

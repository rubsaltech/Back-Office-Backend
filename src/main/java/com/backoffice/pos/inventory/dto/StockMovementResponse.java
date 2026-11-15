package com.backoffice.pos.inventory.dto;

import com.backoffice.pos.inventory.MovementType;
import com.backoffice.pos.inventory.StockMovement;

import java.time.Instant;

/** One ledger entry for the movements timeline. */
public record StockMovementResponse(
        Long id,
        MovementType movementType,
        int quantity,
        int balanceAfter,
        String reason,
        Long orderId,
        Instant createdAt
) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(), m.getMovementType(), m.getQuantity(), m.getBalanceAfter(),
                m.getReason(), m.getOrderId(), m.getCreatedAt());
    }
}

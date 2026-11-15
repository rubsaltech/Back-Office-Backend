package com.backoffice.pos.inventory;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One append-only entry in the stock ledger. Every change to a variant's stock
 * is recorded here with a signed {@link #quantity} and the resulting on-hand
 * ({@link #balanceAfter}). Rows are never updated or deleted — they are the
 * audit trail that explains how a variant reached its current count.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
public class StockMovement extends StoreScopedEntity {

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "unit_id")
    private Long unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /** Signed: positive for receive/return/opening, negative for sale. */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    private String reason;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "performed_by")
    private Long performedBy;
}

package com.backoffice.pos.inventory;

/** The kind of stock movement recorded in the ledger. */
public enum MovementType {
    /** Migrated or initial opening balance. */
    OPENING,
    /** Stock received (purchase/restock). */
    RECEIVE,
    /** Sold on an order. */
    SALE,
    /** Returned to stock (order void). */
    RETURN,
    /** Manual correction to a new on-hand count. */
    ADJUST
}

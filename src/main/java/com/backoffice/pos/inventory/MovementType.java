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
    ADJUST,
    /** Consumed by a service/repair job (part used in a repair). */
    SERVICE_OUT,
    /** Returned to stock after being removed from a service/repair job. */
    SERVICE_RETURN
}

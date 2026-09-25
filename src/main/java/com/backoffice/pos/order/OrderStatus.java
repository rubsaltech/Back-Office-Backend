package com.backoffice.pos.order;

/** Lifecycle of a placed order (see the order-listing statuses in the design). */
public enum OrderStatus {
    CONFIRMED,
    READY_FOR_PICKUP,
    DELIVERED,
    COMPLETED,
    VOID,
    CANCELLED
}

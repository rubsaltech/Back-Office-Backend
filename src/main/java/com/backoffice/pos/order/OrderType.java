package com.backoffice.pos.order;

/** How an order is fulfilled. Drives the cashier flow (table vs. customer). */
public enum OrderType {
    DINE_IN,
    TAKEAWAY,
    DELIVERY
}

package com.backoffice.pos.order;

/**
 * How an order is fulfilled. Which modes apply depends on the store's vertical
 * (see the frontend vertical config): a restaurant uses DINE_IN/TAKEAWAY/
 * DELIVERY, retail uses COUNTER/DELIVERY, a repair shop uses COUNTER/REPAIR, etc.
 */
public enum OrderType {
    DINE_IN,
    TAKEAWAY,
    DELIVERY,
    COUNTER,
    PICKUP,
    SERVICE,
    REPAIR
}

package com.backoffice.pos.store;

/**
 * The business vertical of a store. Drives the POS flow (order modes, whether
 * floors/tables/seats apply, cart visuals). The frontend keeps a matching
 * per-vertical config; the backend just stores and validates the type.
 */
public enum StoreType {
    RESTAURANT,
    RETAIL,
    MOBILE,
    MECHANIC,
    GENERAL
}

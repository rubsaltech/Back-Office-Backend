package com.backoffice.pos.catalog;

/** How a variant's stock is tracked. */
public enum TrackingType {
    /** Plain counted stock (cables, groceries). */
    NONE,
    /** One serialized unit per physical item (phones/IMEI, electronics). */
    SERIAL
}

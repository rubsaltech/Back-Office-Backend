package com.backoffice.pos.business;

/** Lifecycle of a tenant business (managed from the Admin portal). */
public enum BusinessStatus {
    PENDING,
    APPROVED,
    BLOCKED,
    DEACTIVATED
}

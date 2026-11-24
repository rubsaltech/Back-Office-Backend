package com.backoffice.pos.servicejob;

/**
 * Lifecycle stages of a service/repair job. The first five form the normal
 * pipeline (progress = position within them); CANCELLED is an off-pipeline end.
 */
public enum ServiceJobStatus {
    RECEIVED,
    DIAGNOSED,
    IN_PROGRESS,
    READY,
    DELIVERED,
    CANCELLED
}

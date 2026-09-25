package com.backoffice.pos.order;

/** Settlement state of a payment (recorded, not gateway-processed, in v1). */
public enum PaymentStatus {
    PENDING,
    SUCCESSFUL,
    FAILED
}

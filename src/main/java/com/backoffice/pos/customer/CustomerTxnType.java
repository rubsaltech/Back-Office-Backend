package com.backoffice.pos.customer;

/** Direction of a customer ledger entry. */
public enum CustomerTxnType {
    /** The customer paid / was credited (reduces what they owe). */
    CREDIT,
    /** The customer was charged on account (increases what they owe). */
    DEBIT
}

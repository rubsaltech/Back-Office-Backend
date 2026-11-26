package com.backoffice.pos.customer;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** One credit/debit entry in a customer's ledger. */
@Entity
@Table(name = "customer_transactions")
@Getter
@Setter
public class CustomerTransaction extends StoreScopedEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CustomerTxnType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    /** The POS order this entry came from (null for manual adjustments). */
    @Column(name = "order_id")
    private Long orderId;
}

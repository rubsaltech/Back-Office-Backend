package com.backoffice.pos.customer;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A customer of a store. Carries a cached {@link #balance} kept in step with the
 * customer's {@link CustomerTransaction} ledger: positive = the customer owes the
 * store (outstanding debit), negative = the customer is in credit.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer extends StoreScopedEntity {

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    private String address;

    private String city;

    private String country;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
}

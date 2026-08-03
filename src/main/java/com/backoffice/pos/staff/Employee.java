package com.backoffice.pos.staff;

import com.backoffice.pos.common.TenantEntity;
import com.backoffice.pos.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A staff member of a business. Authenticates on the web with email + password,
 * and on the cashier terminal with a numeric PIN (both hashed).
 */
@Entity
@Table(
        name = "employees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "email"})
)
@Getter
@Setter
public class Employee extends TenantEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "pin_hash")
    private String pinHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "sales_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal salesTotal = BigDecimal.ZERO;

    @Column(name = "tips_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal tipsTotal = BigDecimal.ZERO;
}

package com.backoffice.pos.business;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The tenant root. One business owns many stores, employees, roles, etc.
 * The owner authenticates with {@code email} + {@code passwordHash}.
 */
@Entity
@Table(name = "businesses")
@Getter
@Setter
public class Business extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "owner_phone")
    private String ownerPhone;

    @Column(name = "logo_url")
    private String logoUrl;

    private String address;

    private String city;

    private String country;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(nullable = false)
    private String locale = "en-US";

    // --- notification preferences ---
    @Column(name = "notify_orders", nullable = false)
    private boolean notifyOrders = true;

    @Column(name = "notify_low_stock", nullable = false)
    private boolean notifyLowStock = true;

    @Column(name = "notify_reports", nullable = false)
    private boolean notifyReports = false;

    @Column(name = "notify_marketing", nullable = false)
    private boolean notifyMarketing = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessStatus status = BusinessStatus.PENDING;
}

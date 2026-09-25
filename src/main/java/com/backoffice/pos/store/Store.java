package com.backoffice.pos.store;

import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A physical location belonging to a business. One store is the "main" store. */
@Entity
@Table(name = "stores")
@Getter
@Setter
public class Store extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "is_main", nullable = false)
    private boolean main = false;

    /** The vertical (restaurant, retail, …) that drives the POS flow. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreType type = StoreType.RESTAURANT;

    private String phone;

    private String email;

    private String address;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreStatus status = StoreStatus.ACTIVE;
}

package com.backoffice.pos.order;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** A card terminal selectable when taking a Card payment (per business). */
@Entity
@Table(
        name = "payment_devices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "serial_number"})
)
@Getter
@Setter
public class PaymentDevice extends TenantEntity {

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "serial_number", nullable = false)
    private String serialNumber;

    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;
}

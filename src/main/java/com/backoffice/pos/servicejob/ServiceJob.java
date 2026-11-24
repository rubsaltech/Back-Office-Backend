package com.backoffice.pos.servicejob;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** A service/repair ticket tracked by a customer-facing {@link #token}. */
@Entity
@Table(
        name = "service_jobs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "token"})
)
@Getter
@Setter
public class ServiceJob extends StoreScopedEntity {

    @Column(nullable = false, length = 20)
    private String token;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    /** What's being serviced, e.g. "iPhone 12". */
    private String device;

    /** The service requested, e.g. "Battery replacement" (snapshot of the service's name). */
    @Column(name = "service_type")
    private String serviceType;

    /** The catalog service this job was created from (may be null for legacy/ad-hoc jobs). */
    @Column(name = "service_id")
    private Long serviceId;

    /** The base/labour charge, snapshot from the service at creation. */
    @Column(name = "service_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceJobStatus status = ServiceJobStatus.RECEIVED;

    @Column(name = "handler_name")
    private String handlerName;

    /** The POS order this job was billed on (null until billed). */
    @Column(name = "order_id")
    private Long orderId;

    /** When the job was billed at the POS. */
    @Column(name = "billed_at")
    private Instant billedAt;
}

package com.backoffice.pos.servicejob;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A free-form extra charge added to a service job during tracking (beyond the
 * service's base charge) — e.g. extra labour for a harder-than-expected repair.
 * Lands on the final POS receipt alongside the base charge and parts.
 */
@Entity
@Table(name = "service_job_charges")
@Getter
@Setter
public class ServiceJobCharge extends BaseEntity {

    @Column(name = "service_job_id", nullable = false)
    private Long serviceJobId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}

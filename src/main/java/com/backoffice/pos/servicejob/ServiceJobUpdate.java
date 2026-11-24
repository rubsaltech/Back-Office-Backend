package com.backoffice.pos.servicejob;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** One entry in a service job's progress timeline. */
@Entity
@Table(name = "service_job_updates")
@Getter
@Setter
public class ServiceJobUpdate extends BaseEntity {

    @Column(name = "service_job_id", nullable = false)
    private Long serviceJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceJobStatus status;

    @Column(length = 1000)
    private String note;

    @Column(name = "performed_by")
    private String performedBy;
}

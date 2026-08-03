package com.backoffice.pos.common;

import com.backoffice.pos.tenancy.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for all business-scoped entities. Every row carries {@code business_id}
 * (the tenant discriminator). On insert the id is taken from {@link TenantContext}
 * when not already set. Reads are scoped explicitly in the repositories/services.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "business_id", nullable = false, updatable = false)
    private Long businessId;

    @PrePersist
    void assignTenant() {
        if (businessId == null && TenantContext.isSet()) {
            businessId = TenantContext.getBusinessId();
        }
    }
}

package com.backoffice.pos.common;

import com.backoffice.pos.tenancy.StoreContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for entities that belong to a single store (within a business). Carries
 * {@code store_id} on top of {@link TenantEntity}'s {@code business_id}. Services
 * set the store explicitly (via the resolved active store); this {@link PrePersist}
 * is only a fallback for any path that forgets to.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class StoreScopedEntity extends TenantEntity {

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @PrePersist
    void assignStore() {
        if (storeId == null && StoreContext.isSet()) {
            storeId = StoreContext.getStoreId();
        }
    }
}

package com.backoffice.pos.staff;

import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** A fine-grained capability, keyed like {@code product.create}, per business. */
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "permission_key"})
)
@Getter
@Setter
public class Permission extends TenantEntity {

    @Column(name = "permission_key", nullable = false)
    private String key;

    private String description;
}

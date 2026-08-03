package com.backoffice.pos.floor;

import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "floors",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "name"})
)
@Getter
@Setter
public class Floor extends TenantEntity {

    @Column(nullable = false)
    private String name;
}

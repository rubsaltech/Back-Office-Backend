package com.backoffice.pos.label;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom field the business defines once and attaches to products/services.
 * {@link LabelType} decides how the value is captured; {@code SINGLE_SELECT} and
 * {@code MULTI_SELECT} draw from {@link #options}, while {@code INPUT} ignores
 * them (its value is typed per product). Business-scoped (the tenant owns it).
 */
@Entity
@Table(
        name = "labels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "name"})
)
@Getter
@Setter
public class Label extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LabelType type = LabelType.INPUT;

    /** When attached to a product/service, a required label must have a value. */
    @Column(nullable = false)
    private boolean required = false;

    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @OneToMany(mappedBy = "label", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<LabelOption> options = new ArrayList<>();

    public void addOption(LabelOption option) {
        option.setLabel(this);
        options.add(option);
    }

    public void clearOptions() {
        options.clear();
    }
}

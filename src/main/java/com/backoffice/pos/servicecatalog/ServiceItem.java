package com.backoffice.pos.servicecatalog;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A service the business sells (e.g. "Battery Replacement", "Mobile Repair").
 * A service carries its own charge (labour) and may pull in one or more
 * {@link ServiceProduct products} that are consumed by it — both the service
 * line and the linked product lines land on the final POS receipt.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
public class ServiceItem extends StoreScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ServiceProduct> products = new ArrayList<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ServiceItemLabel> labels = new ArrayList<>();

    public void addProduct(ServiceProduct sp) {
        sp.setService(this);
        products.add(sp);
    }

    public void clearProducts() {
        products.clear();
    }

    public void addLabel(ServiceItemLabel label) {
        label.setService(this);
        labels.add(label);
    }

    public void clearLabels() {
        labels.clear();
    }
}

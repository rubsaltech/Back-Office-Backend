package com.backoffice.pos.catalog;

import com.backoffice.pos.common.TenantEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "sku"})
)
@Getter
@Setter
public class Product extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @Column(length = 2000)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_title")
    private String discountTitle;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    // --- stock (the "Inventory" tab is a view over these) ---
    @Column(name = "available_qty", nullable = false)
    private int availableQty = 0;

    @Column(name = "quantity_sold", nullable = false)
    private int quantitySold = 0;

    @Column(name = "total_qty", nullable = false)
    private int totalQty = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductLabel> labels = new ArrayList<>();

    public void addLabel(ProductLabel label) {
        label.setProduct(this);
        labels.add(label);
    }

    public void clearLabels() {
        labels.clear();
    }
}

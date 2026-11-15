package com.backoffice.pos.catalog;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "sku"})
)
@Getter
@Setter
public class Product extends StoreScopedEntity {

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

    /** Tax applied to every variant of this product (shared, product-level). */
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_title")
    private String discountTitle;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    /** True when the product has real, distinct variants (colour/storage); false
     *  when it is a simple item carried by a single hidden {@code isDefault} variant. */
    @Column(name = "has_variants", nullable = false)
    private boolean hasVariants = false;

    // --- the sellable SKUs: price & stock live here, not on the product ---
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductLabel> labels = new ArrayList<>();

    public void addVariant(ProductVariant v) {
        v.setProduct(this);
        variants.add(v);
    }

    /** The default (single-SKU) variant, or the first one if none is flagged. */
    public ProductVariant defaultVariant() {
        return variants.stream()
                .filter(ProductVariant::isDefault)
                .findFirst()
                .orElseGet(() -> variants.isEmpty() ? null : variants.get(0));
    }

    public void addLabel(ProductLabel label) {
        label.setProduct(this);
        labels.add(label);
    }

    public void clearLabels() {
        labels.clear();
    }
}

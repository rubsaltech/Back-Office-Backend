package com.backoffice.pos.catalog;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A sellable SKU under a {@link Product}. This is the real unit of commerce:
 * price, barcode and stock live here, not on the product. A simple product has
 * exactly one {@code isDefault} variant; a phone has one per colour/storage.
 */
@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "sku"})
)
@Getter
@Setter
public class ProductVariant extends StoreScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "variant_name", nullable = false)
    private String variantName = "Default";

    /** Option combination, e.g. {"Color":"Black","Storage":"128GB"}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false)
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "purchased_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasedPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_type", nullable = false, length = 20)
    private TrackingType trackingType = TrackingType.NONE;

    /** Cached on-hand count, kept in step with the stock-movement ledger. */
    @Column(name = "on_hand", nullable = false)
    private int onHand = 0;

    @Column(name = "quantity_sold", nullable = false)
    private int quantitySold = 0;

    @Column(name = "total_received", nullable = false)
    private int totalReceived = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(nullable = false, length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    public boolean isSerialized() {
        return trackingType == TrackingType.SERIAL;
    }
}

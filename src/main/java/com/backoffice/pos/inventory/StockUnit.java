package com.backoffice.pos.inventory;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One physical, tracked unit of a variant. Present only for unit-tracked variants
 * (those whose product has capture-at-stock-in labels). On-hand for such a variant
 * is the count of its units in {@link UnitStatus#IN_STOCK}. The per-unit label
 * values entered at stock-in are held in {@link #attributes} (e.g. IMEI1, IMEI2,
 * battery health); {@link #serial} is a derived, optional display identifier.
 */
@Entity
@Table(name = "stock_units")
@Getter
@Setter
public class StockUnit extends StoreScopedEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(length = 120)
    private String serial;

    /** Per-unit captured label values (key = label name, value = entered value). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitStatus status = UnitStatus.IN_STOCK;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "sold_at")
    private Instant soldAt;
}

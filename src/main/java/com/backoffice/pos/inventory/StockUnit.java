package com.backoffice.pos.inventory;

import com.backoffice.pos.common.StoreScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One physical, serialized unit (an IMEI / serial number) of a variant. Present
 * only for variants whose tracking type is SERIAL. On-hand for such a variant is
 * the count of its units in {@link UnitStatus#IN_STOCK}.
 */
@Entity
@Table(
        name = "stock_units",
        uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "serial"})
)
@Getter
@Setter
public class StockUnit extends StoreScopedEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(nullable = false, length = 120)
    private String serial;

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

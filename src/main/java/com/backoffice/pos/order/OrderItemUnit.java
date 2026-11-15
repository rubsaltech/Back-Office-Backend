package com.backoffice.pos.order;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A permanent record of one serialized unit (IMEI / serial) sold on an order
 * line. This is a snapshot: it survives even after the sold unit is returned to
 * stock (on void) and later re-sold, so an order's invoice always shows exactly
 * which serials it carried.
 */
@Entity
@Table(name = "order_item_units")
@Getter
@Setter
public class OrderItemUnit extends BaseEntity {

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    /** The stock unit sold (nullable: set null if that unit is later deleted). */
    @Column(name = "stock_unit_id")
    private Long stockUnitId;

    @Column(nullable = false, length = 120)
    private String serial;
}

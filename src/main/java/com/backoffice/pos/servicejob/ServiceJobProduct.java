package com.backoffice.pos.servicejob;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A product/part consumed by a service job (e.g. 1 × Panel used in a panel
 * replacement). Stock is drawn down when the row is added. Name and price are
 * snapshotted so the final receipt stays stable even if the catalog changes.
 */
@Entity
@Table(name = "service_job_products")
@Getter
@Setter
public class ServiceJobProduct extends BaseEntity {

    @Column(name = "service_job_id", nullable = false)
    private Long serviceJobId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;
}

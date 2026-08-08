package com.backoffice.pos.servicecatalog;

import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A product consumed by a {@link ServiceItem}, with a quantity (e.g. 1 × Battery). */
@Entity
@Table(name = "service_products")
@Getter
@Setter
public class ServiceProduct extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}

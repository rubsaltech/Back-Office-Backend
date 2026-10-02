package com.backoffice.pos.servicecatalog;

import com.backoffice.pos.common.BaseEntity;
import com.backoffice.pos.label.LabelType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A label attached to a service with its captured value(s). Mirrors
 * {@link com.backoffice.pos.catalog.ProductLabel} — name/type are snapshotted
 * for stable historical display.
 */
@Entity
@Table(name = "service_labels")
@Getter
@Setter
public class ServiceItemLabel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceItem service;

    @Column(name = "label_id")
    private Long labelId;

    @Column(name = "label_name", nullable = false)
    private String labelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_type", nullable = false, length = 20)
    private LabelType labelType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "service_label_values", joinColumns = @JoinColumn(name = "service_label_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "value", length = 1000)
    private List<String> values = new ArrayList<>();
}

package com.backoffice.pos.catalog;

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
 * A label attached to a product with its captured value(s). The label's name
 * and type are snapshotted so historical display stays stable even if the label
 * definition is later renamed or deleted. {@link #values} holds: one entry for
 * INPUT (the typed text) or SINGLE_SELECT, and one-or-more for MULTI_SELECT.
 */
@Entity
@Table(name = "product_labels")
@Getter
@Setter
public class ProductLabel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Reference to the source label definition (nullable if it was deleted). */
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
    @CollectionTable(name = "product_label_values", joinColumns = @JoinColumn(name = "product_label_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "value", length = 1000)
    private List<String> values = new ArrayList<>();
}

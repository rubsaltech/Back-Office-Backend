package com.backoffice.pos.label;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A selectable value within a single/multi-select {@link Label}. */
@Entity
@Table(name = "label_options")
@Getter
@Setter
public class LabelOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @Column(nullable = false)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}

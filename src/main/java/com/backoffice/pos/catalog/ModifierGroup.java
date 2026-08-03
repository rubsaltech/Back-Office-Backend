package com.backoffice.pos.catalog;

import com.backoffice.pos.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A choice group on a product, e.g. "Variations" or "Style". */
@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
public class ModifierGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "min_select", nullable = false)
    private int minSelect = 0;

    @Column(name = "max_select", nullable = false)
    private int maxSelect = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ModifierOption> options = new ArrayList<>();

    public void addOption(ModifierOption option) {
        option.setGroup(this);
        options.add(option);
    }
}

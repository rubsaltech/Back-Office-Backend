package com.backoffice.pos.label.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.label.Label;
import com.backoffice.pos.label.LabelType;

import java.util.List;

public record LabelResponse(
        Long id,
        String name,
        LabelType type,
        boolean required,
        CatalogStatus status,
        List<OptionResponse> options
) {
    public record OptionResponse(Long id, String value, int sortOrder) {
    }

    public static LabelResponse from(Label l) {
        List<OptionResponse> opts = l.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getValue(), o.getSortOrder()))
                .toList();
        return new LabelResponse(l.getId(), l.getName(), l.getType(), l.isRequired(), l.getStatus(), opts);
    }
}

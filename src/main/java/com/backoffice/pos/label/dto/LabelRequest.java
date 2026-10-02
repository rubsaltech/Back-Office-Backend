package com.backoffice.pos.label.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.label.LabelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LabelRequest(
        @NotBlank String name,
        @NotNull LabelType type,
        boolean required,
        CatalogStatus status,
        /** Only used for SINGLE_SELECT / MULTI_SELECT; ignored for INPUT. */
        List<Option> options
) {
    public record Option(String value, Integer sortOrder) {
    }
}

package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        String imageUrl,
        CatalogStatus status
) {
}

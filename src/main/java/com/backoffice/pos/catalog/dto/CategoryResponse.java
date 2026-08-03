package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.Category;

public record CategoryResponse(
        Long id,
        String name,
        String imageUrl,
        CatalogStatus status,
        long productCount
) {
    public static CategoryResponse from(Category c, long productCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getImageUrl(), c.getStatus(), productCount);
    }
}

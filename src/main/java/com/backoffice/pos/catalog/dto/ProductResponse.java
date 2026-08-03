package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.ModifierGroup;
import com.backoffice.pos.catalog.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String barcode,
        String description,
        String imageUrl,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        CatalogStatus status,
        int availableQty,
        int quantitySold,
        int totalQty,
        List<GroupResponse> modifierGroups
) {
    public record GroupResponse(
            Long id, String name, boolean required, int minSelect, int maxSelect, int sortOrder,
            List<OptionResponse> options) {
    }

    public record OptionResponse(Long id, String name, BigDecimal priceDelta, boolean isDefault, int sortOrder) {
    }

    public static ProductResponse from(Product p) {
        List<GroupResponse> groups = p.getModifierGroups().stream()
                .map(ProductResponse::toGroup)
                .toList();
        return new ProductResponse(
                p.getId(), p.getName(), p.getSku(), p.getBarcode(), p.getDescription(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getPrice(), p.getTaxAmount(), p.getDiscountTitle(), p.getDiscountAmount(), p.getStatus(),
                p.getAvailableQty(), p.getQuantitySold(), p.getTotalQty(), groups);
    }

    private static GroupResponse toGroup(ModifierGroup g) {
        List<OptionResponse> opts = g.getOptions().stream()
                .map(o -> new OptionResponse(o.getId(), o.getName(), o.getPriceDelta(), o.isDefault(), o.getSortOrder()))
                .toList();
        return new GroupResponse(g.getId(), g.getName(), g.isRequired(), g.getMinSelect(), g.getMaxSelect(),
                g.getSortOrder(), opts);
    }
}

package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductLabel;
import com.backoffice.pos.label.dto.AttachedLabelResponse;

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
        BigDecimal purchasedPrice,
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        CatalogStatus status,
        int availableQty,
        int quantitySold,
        int totalQty,
        List<AttachedLabelResponse> labels
) {
    public static ProductResponse from(Product p) {
        List<AttachedLabelResponse> labels = p.getLabels().stream()
                .map(ProductResponse::toLabel)
                .toList();
        return new ProductResponse(
                p.getId(), p.getName(), p.getSku(), p.getBarcode(), p.getDescription(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getPrice(), p.getPurchasedPrice(), p.getTaxAmount(), p.getDiscountTitle(), p.getDiscountAmount(), p.getStatus(),
                p.getAvailableQty(), p.getQuantitySold(), p.getTotalQty(), labels);
    }

    private static AttachedLabelResponse toLabel(ProductLabel l) {
        return new AttachedLabelResponse(l.getId(), l.getLabelId(), l.getLabelName(), l.getLabelType(),
                List.copyOf(l.getValues()));
    }
}

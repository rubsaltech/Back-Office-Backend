package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.catalog.ProductLabel;
import com.backoffice.pos.catalog.ProductVariant;
import com.backoffice.pos.label.dto.AttachedLabelResponse;

import java.math.BigDecimal;
import java.util.Comparator;
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
        BigDecimal taxAmount,
        String discountTitle,
        BigDecimal discountAmount,
        CatalogStatus status,
        boolean hasVariants,
        List<VariantResponse> variants,
        List<AttachedLabelResponse> labels,
        List<String> saleCaptureLabels
) {
    public static ProductResponse from(Product p) {
        List<VariantResponse> variants = p.getVariants().stream()
                // default first, then by name, for a stable UI order
                .sorted(Comparator.comparing(ProductVariant::isDefault).reversed()
                        .thenComparing(ProductVariant::getVariantName, Comparator.nullsLast(String::compareTo)))
                .map(VariantResponse::from)
                .toList();
        List<AttachedLabelResponse> labels = p.getLabels().stream()
                .map(ProductResponse::toLabel)
                .toList();
        return new ProductResponse(
                p.getId(), p.getName(), p.getSku(), p.getBarcode(), p.getDescription(), p.getImageUrl(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getTaxAmount(), p.getDiscountTitle(), p.getDiscountAmount(), p.getStatus(),
                p.isHasVariants(), variants, labels,
                p.getSaleCaptureLabels() == null ? List.of() : List.copyOf(p.getSaleCaptureLabels()));
    }

    private static AttachedLabelResponse toLabel(ProductLabel l) {
        return new AttachedLabelResponse(l.getId(), l.getLabelId(), l.getLabelName(), l.getLabelType(),
                List.copyOf(l.getValues()));
    }
}

package com.backoffice.pos.servicecatalog.dto;

import com.backoffice.pos.catalog.CatalogStatus;
import com.backoffice.pos.catalog.Product;
import com.backoffice.pos.label.dto.AttachedLabelResponse;
import com.backoffice.pos.servicecatalog.ServiceItem;
import com.backoffice.pos.servicecatalog.ServiceItemLabel;
import com.backoffice.pos.servicecatalog.ServiceProduct;

import java.math.BigDecimal;
import java.util.List;

public record ServiceItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        CatalogStatus status,
        List<LineResponse> products,
        List<AttachedLabelResponse> labels
) {
    public record LineResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal productPrice,
            int quantity,
            int sortOrder
    ) {
    }

    public static ServiceItemResponse from(ServiceItem s) {
        List<LineResponse> lines = s.getProducts().stream()
                .map(ServiceItemResponse::toLine)
                .toList();
        List<AttachedLabelResponse> labels = s.getLabels().stream()
                .map(ServiceItemResponse::toLabel)
                .toList();
        return new ServiceItemResponse(
                s.getId(), s.getName(), s.getDescription(), s.getPrice(), s.getStatus(), lines, labels);
    }

    private static AttachedLabelResponse toLabel(ServiceItemLabel l) {
        return new AttachedLabelResponse(l.getId(), l.getLabelId(), l.getLabelName(), l.getLabelType(),
                List.copyOf(l.getValues()));
    }

    private static LineResponse toLine(ServiceProduct sp) {
        Product p = sp.getProduct();
        return new LineResponse(
                sp.getId(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                p != null ? p.getPrice() : null,
                sp.getQuantity(),
                sp.getSortOrder());
    }
}

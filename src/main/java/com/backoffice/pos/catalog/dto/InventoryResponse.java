package com.backoffice.pos.catalog.dto;

import com.backoffice.pos.catalog.Product;

import java.math.BigDecimal;

public record InventoryResponse(
        Long productId,
        String name,
        int availableQty,
        BigDecimal price,
        int quantitySold,
        int totalQty
) {
    public static InventoryResponse from(Product p) {
        return new InventoryResponse(p.getId(), p.getName(), p.getAvailableQty(),
                p.getPrice(), p.getQuantitySold(), p.getTotalQty());
    }
}

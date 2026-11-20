package com.backoffice.pos.order.dto;

import com.backoffice.pos.order.DiscountType;
import com.backoffice.pos.order.Order;
import com.backoffice.pos.order.OrderItem;
import com.backoffice.pos.order.OrderStatus;
import com.backoffice.pos.order.OrderType;
import com.backoffice.pos.order.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Full order representation (used by the listing and the invoice). */
public record OrderResponse(
        Long id,
        Long orderNumber,
        OrderType type,
        OrderStatus status,
        Long tableId,
        String tableName,
        int guestCount,
        String customerName,
        String customerPhone,
        String customerAddress,
        String handlerName,
        String kitchenNote,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal subtotal,
        BigDecimal taxTotal,
        BigDecimal discountTotal,
        BigDecimal total,
        Instant createdAt,
        List<Item> items,
        List<PaymentInfo> payments
) {
    public record Item(
            Long id,
            Long productId,
            String productName,
            Long variantId,
            String variantName,
            Integer seatNumber,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal originalUnitPrice,
            BigDecimal taxAmount,
            String specialInstructions,
            BigDecimal lineTotal,
            List<String> serials,
            List<Map<String, String>> captures
    ) {
    }

    public record PaymentInfo(String method, String deviceSerial, BigDecimal amount, String status) {
    }

    public static OrderResponse from(Order o) {
        return from(o, Map.of());
    }

    public static OrderResponse from(Order o, Map<Long, List<String>> serialsByItem) {
        List<Item> items = o.getItems().stream()
                .map(i -> toItem(i, serialsByItem.getOrDefault(i.getId(), List.of())))
                .toList();
        List<PaymentInfo> payments = o.getPayments().stream()
                .map(p -> new PaymentInfo(p.getMethod().name(), p.getDeviceSerial(), p.getAmount(), p.getStatus().name()))
                .toList();
        return new OrderResponse(
                o.getId(), o.getOrderNumber(), o.getType(), o.getStatus(),
                o.getTableId(), o.getTableName(), o.getGuestCount(),
                o.getCustomerName(), o.getCustomerPhone(), o.getCustomerAddress(),
                o.getHandlerName(), o.getKitchenNote(),
                o.getDiscountType(), o.getDiscountValue(),
                o.getSubtotal(), o.getTaxTotal(), o.getDiscountTotal(), o.getTotal(),
                o.getCreatedAt(), items, payments);
    }

    private static Item toItem(OrderItem i, List<String> serials) {
        return new Item(i.getId(), i.getProductId(), i.getProductName(), i.getVariantId(), i.getVariantName(),
                i.getSeatNumber(), i.getQuantity(), i.getUnitPrice(), i.getOriginalUnitPrice(), i.getTaxAmount(),
                i.getSpecialInstructions(), i.getLineTotal(), serials,
                i.getCaptures() == null ? List.of() : i.getCaptures());
    }
}

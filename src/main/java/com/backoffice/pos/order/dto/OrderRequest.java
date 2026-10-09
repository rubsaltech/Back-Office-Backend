package com.backoffice.pos.order.dto;

import com.backoffice.pos.order.DiscountType;
import com.backoffice.pos.order.OrderType;
import com.backoffice.pos.order.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Payload for placing (confirming) a cashier order in one call. */
public record OrderRequest(
        @NotNull OrderType type,
        Long tableId,
        Integer guestCount,
        String customerName,
        String customerPhone,
        String customerAddress,
        String kitchenNote,
        DiscountType discountType,
        BigDecimal discountValue,
        @NotEmpty List<Line> items,
        Payment payment
) {
    /** A cart line: a product, quantity, optional seat, and an optional manual
     *  unit-price override (per this line only; the product's catalog price is
     *  unchanged). When null, the product's current price is used. */
    public record Line(
            @NotNull Long productId,
            Integer seatNumber,
            Integer quantity,
            String specialInstructions,
            BigDecimal unitPrice
    ) {
    }

    /** Optional payment captured at confirmation. */
    public record Payment(
            PaymentMethod method,
            Long deviceId
    ) {
    }
}

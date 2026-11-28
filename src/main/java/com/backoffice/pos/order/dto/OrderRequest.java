package com.backoffice.pos.order.dto;

import com.backoffice.pos.order.DiscountType;
import com.backoffice.pos.order.OrderType;
import com.backoffice.pos.order.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Payload for placing (confirming) a cashier order in one call. */
public record OrderRequest(
        @NotNull OrderType type,
        Long tableId,
        Integer guestCount,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerAddress,
        BigDecimal amountReceived,
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
            Long variantId,
            Integer seatNumber,
            Integer quantity,
            String specialInstructions,
            BigDecimal unitPrice,
            List<Long> unitIds,
            /** Capture-at-sale values, one map per unit: [{"IMEI1":"…"}, …]. */
            List<Map<String, String>> captures
    ) {
    }

    /** Optional payment captured at confirmation. */
    public record Payment(
            PaymentMethod method,
            Long deviceId
    ) {
    }
}

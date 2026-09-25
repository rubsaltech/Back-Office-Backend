package com.backoffice.pos.order.dto;

import com.backoffice.pos.order.OrderStatus;
import com.backoffice.pos.order.PaymentDevice;
import jakarta.validation.constraints.NotNull;

/** Small grouped DTOs for the order module. */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** Compact row for the order-listing table. */
    public record OrderSummary(
            Long id,
            Long orderNumber,
            String tableName,
            String handlerName,
            String type,
            OrderStatus status,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal total,
            java.time.Instant createdAt
    ) {
        public static OrderSummary from(com.backoffice.pos.order.Order o) {
            return new OrderSummary(o.getId(), o.getOrderNumber(), o.getTableName(), o.getHandlerName(),
                    o.getType().name(), o.getStatus(), o.getSubtotal(), o.getTotal(), o.getCreatedAt());
        }
    }

    public record StatusUpdate(@NotNull OrderStatus status) {
    }

    public record PaymentDeviceResponse(Long id, String serialNumber, String label) {
        public static PaymentDeviceResponse from(PaymentDevice d) {
            return new PaymentDeviceResponse(d.getId(), d.getSerialNumber(), d.getLabel());
        }
    }

    public record PaymentDeviceRequest(
            @jakarta.validation.constraints.NotBlank String serialNumber,
            String label,
            Long storeId
    ) {
    }
}

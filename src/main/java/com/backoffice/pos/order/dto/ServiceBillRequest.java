package com.backoffice.pos.order.dto;

import com.backoffice.pos.order.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal payload to bill a service/repair job at the POS. Builds an order from
 * the service's base charge plus the parts consumed. Stock is NOT drawn here —
 * parts were already consumed when added to the job.
 */
public record ServiceBillRequest(
        String serviceName,
        BigDecimal serviceCharge,
        Long customerId,
        String customerName,
        String customerPhone,
        BigDecimal amountReceived,
        List<BillLine> lines,
        PaymentMethod paymentMethod,
        Long deviceId
) {
    public record BillLine(
            Long productId,
            Long variantId,
            String productName,
            String variantName,
            int quantity,
            BigDecimal unitPrice
    ) {
    }
}

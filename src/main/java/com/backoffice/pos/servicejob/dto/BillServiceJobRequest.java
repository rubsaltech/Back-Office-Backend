package com.backoffice.pos.servicejob.dto;

import com.backoffice.pos.order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/** Bill a service job at the POS: creates the order (service charge + parts) and closes the job. */
public record BillServiceJobRequest(
        @NotBlank String token,
        PaymentMethod paymentMethod,
        Long deviceId,
        Long customerId,
        BigDecimal amountReceived
) {
}

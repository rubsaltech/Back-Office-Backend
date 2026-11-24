package com.backoffice.pos.servicejob.dto;

import com.backoffice.pos.servicejob.ServiceJob;
import com.backoffice.pos.servicejob.ServiceJobCharge;
import com.backoffice.pos.servicejob.ServiceJobProduct;
import com.backoffice.pos.servicejob.ServiceJobStatus;
import com.backoffice.pos.servicejob.ServiceJobUpdate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ServiceJobResponse(
        Long id,
        String token,
        String customerName,
        String customerPhone,
        String device,
        String serviceType,
        Long serviceId,
        BigDecimal serviceCharge,
        ServiceJobStatus status,
        String handlerName,
        boolean billed,
        Long orderId,
        Instant billedAt,
        Instant createdAt,
        List<ProductLine> products,
        BigDecimal productsTotal,
        List<ChargeLine> extraCharges,
        BigDecimal extraChargesTotal,
        BigDecimal grandTotal,
        List<UpdateInfo> updates
) {
    public record UpdateInfo(
            Long id,
            ServiceJobStatus status,
            String note,
            String performedBy,
            Instant createdAt
    ) {
    }

    public record ProductLine(
            Long id,
            Long productId,
            Long variantId,
            String productName,
            String variantName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
        static ProductLine from(ServiceJobProduct p) {
            BigDecimal unit = p.getUnitPrice() == null ? BigDecimal.ZERO : p.getUnitPrice();
            return new ProductLine(
                    p.getId(), p.getProductId(), p.getVariantId(), p.getProductName(), p.getVariantName(),
                    p.getQuantity(), unit, unit.multiply(BigDecimal.valueOf(p.getQuantity())));
        }
    }

    public record ChargeLine(Long id, String title, BigDecimal amount) {
        static ChargeLine from(ServiceJobCharge c) {
            return new ChargeLine(c.getId(), c.getTitle(), c.getAmount() == null ? BigDecimal.ZERO : c.getAmount());
        }
    }

    public static ServiceJobResponse from(ServiceJob j, List<ServiceJobUpdate> updates,
                                          List<ServiceJobProduct> products, List<ServiceJobCharge> charges) {
        List<UpdateInfo> timeline = updates.stream()
                .map(u -> new UpdateInfo(u.getId(), u.getStatus(), u.getNote(), u.getPerformedBy(), u.getCreatedAt()))
                .toList();
        List<ProductLine> lines = products.stream().map(ProductLine::from).toList();
        BigDecimal productsTotal = lines.stream()
                .map(ProductLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ChargeLine> extra = charges.stream().map(ChargeLine::from).toList();
        BigDecimal extraTotal = extra.stream()
                .map(ChargeLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal charge = j.getServiceCharge() == null ? BigDecimal.ZERO : j.getServiceCharge();
        return new ServiceJobResponse(
                j.getId(), j.getToken(), j.getCustomerName(), j.getCustomerPhone(),
                j.getDevice(), j.getServiceType(), j.getServiceId(), charge,
                j.getStatus(), j.getHandlerName(),
                j.getOrderId() != null, j.getOrderId(), j.getBilledAt(),
                j.getCreatedAt(), lines, productsTotal, extra, extraTotal,
                charge.add(productsTotal).add(extraTotal), timeline);
    }

    /** Lightweight summary (for lists — no timeline, products or charges). */
    public static ServiceJobResponse summary(ServiceJob j) {
        return from(j, List.of(), List.of(), List.of());
    }
}

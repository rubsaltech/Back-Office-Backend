package com.backoffice.pos.order;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceRequest;
import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentDeviceService {

    private final PaymentDeviceRepository devices;

    public PaymentDeviceService(PaymentDeviceRepository devices) {
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    public List<PaymentDeviceResponse> list() {
        return devices.findByBusinessIdOrderByCreatedAtAsc(TenantContext.requireBusinessId())
                .stream().map(PaymentDeviceResponse::from).toList();
    }

    @Transactional
    public PaymentDeviceResponse create(PaymentDeviceRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        if (devices.existsByBusinessIdAndSerialNumber(businessId, req.serialNumber())) {
            throw new ConflictException("A device with this serial already exists: " + req.serialNumber());
        }
        PaymentDevice device = new PaymentDevice();
        device.setBusinessId(businessId);
        device.setSerialNumber(req.serialNumber());
        device.setLabel(req.label());
        device.setStoreId(req.storeId());
        return PaymentDeviceResponse.from(devices.save(device));
    }
}

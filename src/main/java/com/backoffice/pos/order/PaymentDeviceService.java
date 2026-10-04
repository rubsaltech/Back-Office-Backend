package com.backoffice.pos.order;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceRequest;
import com.backoffice.pos.order.dto.OrderDtos.PaymentDeviceResponse;
import com.backoffice.pos.store.StoreResolver;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentDeviceService {

    private final PaymentDeviceRepository devices;
    private final StoreResolver storeResolver;

    public PaymentDeviceService(PaymentDeviceRepository devices, StoreResolver storeResolver) {
        this.devices = devices;
        this.storeResolver = storeResolver;
    }

    @Transactional(readOnly = true)
    public List<PaymentDeviceResponse> list() {
        return devices.findByStoreIdOrderByCreatedAtAsc(storeResolver.currentStoreId())
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
        // Default a device with no explicit store to the active store.
        device.setStoreId(req.storeId() != null ? req.storeId() : storeResolver.currentStoreId());
        device.setSerialNumber(req.serialNumber());
        device.setLabel(req.label());
        return PaymentDeviceResponse.from(devices.save(device));
    }
}

package com.backoffice.pos.store;

import com.backoffice.pos.common.exception.ConflictException;
import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.order.PaymentDevice;
import com.backoffice.pos.order.PaymentDeviceRepository;
import com.backoffice.pos.store.dto.StoreRequest;
import com.backoffice.pos.store.dto.StoreResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreService {

    private final StoreRepository stores;
    private final PaymentDeviceRepository paymentDevices;

    public StoreService(StoreRepository stores, PaymentDeviceRepository paymentDevices) {
        this.stores = stores;
        this.paymentDevices = paymentDevices;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> list() {
        return stores.findByBusinessIdOrderByCreatedAtAsc(TenantContext.requireBusinessId())
                .stream().map(StoreResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse get(Long id) {
        return StoreResponse.from(load(id));
    }

    @Transactional
    public StoreResponse create(StoreRequest req) {
        Long businessId = TenantContext.requireBusinessId();
        List<Store> existing = stores.findByBusinessIdOrderByCreatedAtAsc(businessId);

        Store store = new Store();
        store.setBusinessId(businessId);
        apply(store, req);
        // The very first store is the "main" store by default.
        if (existing.isEmpty()) {
            store.setMain(true);
        }
        Store saved = stores.save(store);

        // Seed a default card terminal on the first store so Card payments work.
        if (existing.isEmpty()
                && !paymentDevices.existsByBusinessIdAndSerialNumber(businessId, "0821595192")) {
            PaymentDevice device = new PaymentDevice();
            device.setBusinessId(businessId);
            device.setStoreId(saved.getId());
            device.setSerialNumber("0821595192");
            device.setLabel("Main Terminal");
            paymentDevices.save(device);
        }
        return StoreResponse.from(saved);
    }

    @Transactional
    public StoreResponse update(Long id, StoreRequest req) {
        Store store = load(id);
        apply(store, req);
        Store saved = stores.save(store);
        // Exactly one store is "main": promoting this one demotes the others.
        if (saved.isMain()) {
            stores.findByBusinessIdOrderByCreatedAtAsc(saved.getBusinessId()).stream()
                    .filter(s -> !s.getId().equals(saved.getId()) && s.isMain())
                    .forEach(s -> { s.setMain(false); stores.save(s); });
        }
        return StoreResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Store store = load(id);
        List<Store> all = stores.findByBusinessIdOrderByCreatedAtAsc(store.getBusinessId());
        if (all.size() <= 1) {
            throw new ConflictException("You cannot delete your only store");
        }
        boolean wasMain = store.isMain();
        stores.delete(store);
        // If the main store was removed, promote the earliest remaining one.
        if (wasMain) {
            all.stream().filter(s -> !s.getId().equals(id)).findFirst().ifPresent(s -> {
                s.setMain(true);
                stores.save(s);
            });
        }
    }

    private Store load(Long id) {
        return stores.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Store", id));
    }

    private void apply(Store store, StoreRequest req) {
        store.setName(req.name());
        if (req.type() != null) {
            store.setType(req.type());
        }
        store.setPhone(req.phone());
        store.setEmail(req.email());
        store.setMain(req.main());
        store.setAddress(req.address());
        store.setLatitude(req.latitude());
        store.setLongitude(req.longitude());
        if (req.status() != null) {
            store.setStatus(req.status());
        }
    }
}

package com.backoffice.pos.store;

import com.backoffice.pos.common.exception.NotFoundException;
import com.backoffice.pos.store.dto.StoreRequest;
import com.backoffice.pos.store.dto.StoreResponse;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreService {

    private final StoreRepository stores;

    public StoreService(StoreRepository stores) {
        this.stores = stores;
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
        Store store = new Store();
        store.setBusinessId(TenantContext.requireBusinessId());
        apply(store, req);
        return StoreResponse.from(stores.save(store));
    }

    @Transactional
    public StoreResponse update(Long id, StoreRequest req) {
        Store store = load(id);
        apply(store, req);
        return StoreResponse.from(stores.save(store));
    }

    @Transactional
    public void delete(Long id) {
        stores.delete(load(id));
    }

    private Store load(Long id) {
        return stores.findByIdAndBusinessId(id, TenantContext.requireBusinessId())
                .orElseThrow(() -> NotFoundException.of("Store", id));
    }

    private void apply(Store store, StoreRequest req) {
        store.setName(req.name());
        store.setMain(req.main());
        store.setAddress(req.address());
        store.setLatitude(req.latitude());
        store.setLongitude(req.longitude());
        if (req.status() != null) {
            store.setStatus(req.status());
        }
    }
}

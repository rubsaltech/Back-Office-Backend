package com.backoffice.pos.store;

import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.tenancy.StoreContext;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves the store that store-scoped operations should act on for the current
 * request. Uses the {@code X-Store-Id} header (via {@link StoreContext}) when it
 * names a store that belongs to the caller's business; otherwise falls back to
 * the business's main store. The fallback keeps the app working during the brief
 * window before the UI has selected a store, and for clients that send no header.
 */
@Component
public class StoreResolver {

    private final StoreRepository stores;

    public StoreResolver(StoreRepository stores) {
        this.stores = stores;
    }

    /** @return the id of the store to scope the current request to. */
    public Long currentStoreId() {
        Long businessId = TenantContext.requireBusinessId();

        Long requested = StoreContext.getStoreId();
        if (requested != null && stores.existsByIdAndBusinessId(requested, businessId)) {
            return requested;
        }

        return stores.findFirstByBusinessIdAndMainTrue(businessId)
                .or(() -> stores.findFirstByBusinessIdOrderByCreatedAtAsc(businessId))
                .map(Store::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "No store exists for this business yet"));
    }
}

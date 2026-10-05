package com.backoffice.pos.store;

import com.backoffice.pos.auth.PrincipalType;
import com.backoffice.pos.common.exception.ApiException;
import com.backoffice.pos.security.AuthPrincipal;
import com.backoffice.pos.security.CurrentUser;
import com.backoffice.pos.staff.Employee;
import com.backoffice.pos.staff.EmployeeRepository;
import com.backoffice.pos.tenancy.StoreContext;
import com.backoffice.pos.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves the store that store-scoped operations should act on for the current
 * request. Uses the {@code X-Store-Id} header (via {@link StoreContext}) when it
 * names a store the caller is allowed to use, else falls back to a default.
 *
 * <p>An <b>owner</b> may use any store in their business (fallback: the main
 * store). An <b>employee</b> may only use a store they are assigned to (fallback:
 * their first assigned store) — so a forged header cannot reach a store they are
 * not a member of, even within their own business.
 */
@Component
public class StoreResolver {

    private final StoreRepository stores;
    private final EmployeeRepository employees;

    public StoreResolver(StoreRepository stores, EmployeeRepository employees) {
        this.stores = stores;
        this.employees = employees;
    }

    /** @return the id of the store to scope the current request to. */
    public Long currentStoreId() {
        Long businessId = TenantContext.requireBusinessId();
        Long requested = StoreContext.getStoreId();
        AuthPrincipal principal = CurrentUser.get();

        if (principal.type() == PrincipalType.EMPLOYEE) {
            Employee employee = employees.findByIdAndBusinessId(principal.id(), businessId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Employee not found"));
            if (requested != null) {
                boolean allowed = employee.getStores().stream().anyMatch(s -> s.getId().equals(requested));
                if (allowed) {
                    return requested;
                }
            }
            return employee.getStores().stream().map(Store::getId).findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "You are not assigned to any store"));
        }

        // Owner: any store in the business; fall back to the main store.
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

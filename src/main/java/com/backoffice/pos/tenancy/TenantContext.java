package com.backoffice.pos.tenancy;

/**
 * Holds the current request's tenant (business) id in a thread-local, populated
 * by the JWT auth filter. Services read it to scope all data access to the
 * caller's business. Platform-admin requests may leave it unset.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_BUSINESS = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setBusinessId(Long businessId) {
        CURRENT_BUSINESS.set(businessId);
    }

    /** @return current business id, or {@code null} if none is bound. */
    public static Long getBusinessId() {
        return CURRENT_BUSINESS.get();
    }

    public static boolean isSet() {
        return CURRENT_BUSINESS.get() != null;
    }

    /** @return the current business id or throws if none is bound. */
    public static Long requireBusinessId() {
        Long id = CURRENT_BUSINESS.get();
        if (id == null) {
            throw new IllegalStateException("No tenant (business) bound to the current request");
        }
        return id;
    }

    public static void clear() {
        CURRENT_BUSINESS.remove();
    }
}

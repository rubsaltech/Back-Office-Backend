package com.backoffice.pos.tenancy;

/**
 * Holds the current request's active store id in a thread-local, populated from
 * the {@code X-Store-Id} header by the JWT auth filter. Store-scoped services
 * read it to narrow data to the selected store (within the caller's business).
 *
 * <p>The raw header value is trusted only as an id; services still resolve it
 * through {@code findByIdAndBusinessId}, so a client cannot reach another
 * business's store by sending a forged header.
 */
public final class StoreContext {

    private static final ThreadLocal<Long> CURRENT_STORE = new ThreadLocal<>();

    private StoreContext() {
    }

    public static void setStoreId(Long storeId) {
        CURRENT_STORE.set(storeId);
    }

    /** @return current store id, or {@code null} if none is bound. */
    public static Long getStoreId() {
        return CURRENT_STORE.get();
    }

    public static boolean isSet() {
        return CURRENT_STORE.get() != null;
    }

    /** @return the current store id or throws if none is bound. */
    public static Long requireStoreId() {
        Long id = CURRENT_STORE.get();
        if (id == null) {
            throw new IllegalStateException("No active store bound to the current request");
        }
        return id;
    }

    public static void clear() {
        CURRENT_STORE.remove();
    }
}

-- ============================================================================
-- V14 — Floors, tables and orders scoped per store (Phase 3).
-- Existing rows are assigned to their business's MAIN store (earliest fallback).
-- Order NUMBERING stays per-business; orders are only filtered by store, so the
-- orders.store_id column (added nullable in V7) is just backfilled, not made NOT
-- NULL — deleting a store SETs NULL there to preserve order history.
-- ============================================================================

-- ---- floors ----
ALTER TABLE floors ADD COLUMN IF NOT EXISTS store_id BIGINT;
UPDATE floors f SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = f.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
ALTER TABLE floors ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE floors ADD CONSTRAINT fk_floors_store
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE;
CREATE INDEX idx_floors_store ON floors (store_id);
ALTER TABLE floors DROP CONSTRAINT IF EXISTS uq_floor_business_name;
ALTER TABLE floors ADD CONSTRAINT uq_floor_store_name UNIQUE (store_id, name);

-- ---- restaurant_tables ----
ALTER TABLE restaurant_tables ADD COLUMN IF NOT EXISTS store_id BIGINT;
UPDATE restaurant_tables t SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = t.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
ALTER TABLE restaurant_tables ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE restaurant_tables ADD CONSTRAINT fk_tables_store
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE;
CREATE INDEX idx_tables_store ON restaurant_tables (store_id);

-- ---- orders (store_id already exists, nullable) ----
UPDATE orders o SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = o.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_orders_store ON orders (store_id);

-- ---- payment_devices (store_id already exists, nullable) — backfill so the
-- POS device picker (now store-scoped) still shows existing terminals. ----
UPDATE payment_devices d SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = d.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
CREATE INDEX IF NOT EXISTS idx_payment_devices_store ON payment_devices (store_id);

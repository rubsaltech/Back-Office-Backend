-- ============================================================================
-- V13 — Catalog per store. Products, categories and services become scoped to a
-- single store (not just a business). Existing rows are assigned to their
-- business's MAIN store (falling back to its earliest store). SKU/name
-- uniqueness moves from per-business to per-store.
-- ============================================================================

-- Helper note: each business currently has exactly one store, so backfilling to
-- the main store preserves all existing uniqueness (no constraint violations).

-- ---- products ----
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id BIGINT;
UPDATE products p SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = p.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
ALTER TABLE products ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_store
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE;
CREATE INDEX idx_products_store ON products (store_id);
ALTER TABLE products DROP CONSTRAINT IF EXISTS uq_product_business_sku;
ALTER TABLE products ADD CONSTRAINT uq_product_store_sku UNIQUE (store_id, sku);

-- ---- categories ----
ALTER TABLE categories ADD COLUMN IF NOT EXISTS store_id BIGINT;
UPDATE categories c SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = c.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
ALTER TABLE categories ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE categories ADD CONSTRAINT fk_categories_store
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE;
CREATE INDEX idx_categories_store ON categories (store_id);
ALTER TABLE categories DROP CONSTRAINT IF EXISTS uq_category_business_name;
ALTER TABLE categories ADD CONSTRAINT uq_category_store_name UNIQUE (store_id, name);

-- ---- services ----
ALTER TABLE services ADD COLUMN IF NOT EXISTS store_id BIGINT;
UPDATE services sv SET store_id = (
    SELECT s.id FROM stores s WHERE s.business_id = sv.business_id
    ORDER BY s.is_main DESC, s.created_at ASC
    LIMIT 1
) WHERE store_id IS NULL;
ALTER TABLE services ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE services ADD CONSTRAINT fk_services_store
    FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE;
CREATE INDEX idx_services_store ON services (store_id);

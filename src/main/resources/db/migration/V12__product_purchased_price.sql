-- ============================================================================
-- V12 — Formalize products.purchased_price (cost price).
-- The column was added manually; this pins it to the entity contract
-- (NUMERIC(12,2) NOT NULL DEFAULT 0) so Hibernate `validate` passes regardless
-- of how it was originally created. Idempotent — safe whether or not it exists.
-- ============================================================================

ALTER TABLE products ADD COLUMN IF NOT EXISTS purchased_price NUMERIC(12, 2);

-- Coerce whatever type/precision it has into NUMERIC(12,2).
ALTER TABLE products
    ALTER COLUMN purchased_price TYPE NUMERIC(12, 2)
    USING COALESCE(purchased_price, 0)::NUMERIC(12, 2);

UPDATE products SET purchased_price = 0 WHERE purchased_price IS NULL;

ALTER TABLE products ALTER COLUMN purchased_price SET DEFAULT 0;
ALTER TABLE products ALTER COLUMN purchased_price SET NOT NULL;

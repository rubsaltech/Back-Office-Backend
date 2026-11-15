-- ============================================================================
-- V21 — Order lines reference the sold variant.
--
-- An order line now records which variant was sold (snapshotted variant_name,
-- like product_name). Existing lines are backfilled to the referenced product's
-- Default variant so history stays linked.
-- ============================================================================

ALTER TABLE order_items ADD COLUMN variant_id   BIGINT REFERENCES product_variants (id) ON DELETE SET NULL;
ALTER TABLE order_items ADD COLUMN variant_name VARCHAR(255);

UPDATE order_items oi
SET variant_id   = v.id,
    variant_name = v.variant_name
FROM product_variants v
WHERE v.product_id = oi.product_id
  AND v.is_default = TRUE
  AND oi.product_id IS NOT NULL;

-- ============================================================================
-- V26 — Move tax + discount from the product down to each variant.
--
-- Each variant now carries its own tax and (catalog) discount. Existing product
-- level values are backfilled onto every variant so nothing changes for current
-- data. The product columns are kept (unused by the new product form).
-- ============================================================================

ALTER TABLE product_variants ADD COLUMN tax_amount      NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE product_variants ADD COLUMN discount_title  VARCHAR(255);
ALTER TABLE product_variants ADD COLUMN discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;

UPDATE product_variants v
SET tax_amount      = COALESCE(p.tax_amount, 0),
    discount_title  = p.discount_title,
    discount_amount = COALESCE(p.discount_amount, 0)
FROM products p
WHERE v.product_id = p.id;

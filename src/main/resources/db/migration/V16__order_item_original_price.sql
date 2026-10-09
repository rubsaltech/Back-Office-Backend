-- ============================================================================
-- V16 — Keep the original (catalog) unit price on each order line, so receipts
-- can show it struck-through next to a manually overridden price.
-- Existing lines had no override, so their original price = their unit price.
-- ============================================================================

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS original_unit_price NUMERIC(12, 2);
UPDATE order_items SET original_unit_price = unit_price WHERE original_unit_price IS NULL;

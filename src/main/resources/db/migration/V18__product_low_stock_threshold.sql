-- ============================================================================
-- V18 — Per-product low-stock reorder point. A product is "low stock" when
-- available_qty <= low_stock_threshold. Defaults to 5 for existing products.
-- ============================================================================

ALTER TABLE products ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER NOT NULL DEFAULT 5;

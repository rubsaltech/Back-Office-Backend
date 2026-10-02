-- ============================================================================
-- V10 — Remove the product "Customizations" (modifiers) feature entirely.
-- Products now carry only Labels (V9). Order lines keep their price snapshot;
-- the per-line modifier snapshots are dropped along with the catalog tables.
-- ============================================================================

DROP TABLE IF EXISTS order_item_modifiers;
DROP TABLE IF EXISTS modifier_options;
DROP TABLE IF EXISTS modifier_groups;

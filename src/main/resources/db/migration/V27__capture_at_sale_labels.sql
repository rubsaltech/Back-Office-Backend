-- ============================================================================
-- V27 — "Capture at sale" labels (e.g. IMEI1, IMEI2).
--
-- A label can be flagged capture_at_sale: its value is NOT entered when the
-- product is created, only at the POS, once per unit sold. A product records
-- which such labels it carries; each order line records the captured values.
-- ============================================================================

ALTER TABLE labels      ADD COLUMN capture_at_sale     BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products    ADD COLUMN sale_capture_labels JSONB   NOT NULL DEFAULT '[]';
ALTER TABLE order_items ADD COLUMN captures            JSONB;

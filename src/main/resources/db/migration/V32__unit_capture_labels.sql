-- ============================================================================
-- V32 — Per-unit label capture at stock-in (replaces capture-at-sale).
--
-- Labels gain a new category "capture at stock-in": per-unit values entered when
-- stock is RECEIVED (e.g. IMEI1, IMEI2, battery health), with an optional default.
-- A unit now stores a map of those values (attributes); its serial is derived and
-- optional. The old capture-at-sale flag is retired (carried over to stock-in).
-- ============================================================================

ALTER TABLE labels
    ADD COLUMN capture_at_stock_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN default_value       VARCHAR(255);

-- Carry existing capture-at-sale labels (e.g. IMEI1/IMEI2) over to the new category.
UPDATE labels SET capture_at_stock_in = TRUE WHERE capture_at_sale = TRUE;

-- Stock units: per-unit captured values; serial is now derived/optional (not unique).
ALTER TABLE stock_units
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}';
ALTER TABLE stock_units DROP CONSTRAINT IF EXISTS uq_unit_business_serial;
ALTER TABLE stock_units ALTER COLUMN serial DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_units_business_serial ON stock_units (business_id, serial);

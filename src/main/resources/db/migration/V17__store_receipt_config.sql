-- ============================================================================
-- V17 — Per-store receipt customization. Stores a JSON config (owned by the
-- client) used to tailor the printed receipt (store name override, header/footer
-- text, logo, section toggles). NULL means "use the built-in default layout".
-- ============================================================================

ALTER TABLE stores ADD COLUMN IF NOT EXISTS receipt_config VARCHAR(20000);

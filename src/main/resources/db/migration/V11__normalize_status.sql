-- ============================================================================
-- V11 — Normalize CatalogStatus values to uppercase.
-- Bulk/manually-inserted rows stored status in the wrong case (e.g. 'active'),
-- which broke entity hydration ("No enum constant CatalogStatus.active") and
-- made status counts/filters miss those rows. The app now also reads the enum
-- case-insensitively (CatalogStatusConverter), but the stored data must be
-- uppercase so equality checks against 'ACTIVE'/'INACTIVE' match.
-- ============================================================================

UPDATE products        SET status = UPPER(status) WHERE status <> UPPER(status);
UPDATE categories      SET status = UPPER(status) WHERE status <> UPPER(status);
UPDATE services        SET status = UPPER(status) WHERE status <> UPPER(status);
UPDATE labels          SET status = UPPER(status) WHERE status <> UPPER(status);
UPDATE payment_devices SET status = UPPER(status) WHERE status <> UPPER(status);

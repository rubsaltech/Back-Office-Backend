-- ============================================================================
-- V8 — Store vertical: type + per-store contact (phone/email). The store type
-- drives which POS order modes and cart UI are shown. Existing stores are the
-- restaurant build, so they default to RESTAURANT.
-- ============================================================================

ALTER TABLE stores
    ADD COLUMN type  VARCHAR(20)  NOT NULL DEFAULT 'RESTAURANT',
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN email VARCHAR(255);

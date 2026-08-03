-- ============================================================================
-- V4 — Business settings: address + notification preferences
-- ============================================================================

ALTER TABLE businesses
    ADD COLUMN address          VARCHAR(512),
    ADD COLUMN city             VARCHAR(150),
    ADD COLUMN country          VARCHAR(150),
    ADD COLUMN notify_orders    BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notify_low_stock BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN notify_reports   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN notify_marketing BOOLEAN NOT NULL DEFAULT FALSE;

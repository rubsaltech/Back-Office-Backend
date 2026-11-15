-- ============================================================================
-- V22 — Align stock_movements with BaseEntity (it carries updated_at).
-- The ledger is still append-only in practice; the column just satisfies the
-- shared audit mapping and is set on insert.
-- ============================================================================

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

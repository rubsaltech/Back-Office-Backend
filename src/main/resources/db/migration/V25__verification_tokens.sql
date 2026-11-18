-- ============================================================================
-- V25 — Switch signup + password-reset from short OTP codes to link tokens.
--
-- The emailed link carries a long opaque token; we store only its SHA-256 hash
-- and look rows up by that hash. The column is renamed code_hash -> token_hash
-- to reflect this (same storage, different semantics).
-- ============================================================================

ALTER TABLE signup_otps        RENAME COLUMN code_hash TO token_hash;
ALTER TABLE password_reset_otps RENAME COLUMN code_hash TO token_hash;

CREATE INDEX idx_signup_otp_token         ON signup_otps (token_hash);
CREATE INDEX idx_password_reset_token     ON password_reset_otps (token_hash);

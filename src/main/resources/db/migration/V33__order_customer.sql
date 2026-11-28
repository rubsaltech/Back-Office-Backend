-- ============================================================================
-- V33 — Attach a customer to an order (optional).
--
-- At POS checkout the cashier may pick/create a customer (or leave it blank for
-- a walk-in). When the customer underpays, the balance is logged to their
-- credit/debit ledger (customer_transactions) — see CustomerService.
-- ============================================================================

ALTER TABLE orders
    ADD COLUMN customer_id BIGINT REFERENCES customers (id) ON DELETE SET NULL;

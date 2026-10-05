-- ============================================================================
-- V15 — Employees work in MULTIPLE stores (Phase 4).
-- Replaces the single employees.store_id with an employee_stores join table.
-- Existing single assignments are migrated over, then the old column is dropped.
-- ============================================================================

CREATE TABLE employee_stores (
    employee_id BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    store_id    BIGINT NOT NULL REFERENCES stores (id)    ON DELETE CASCADE,
    PRIMARY KEY (employee_id, store_id)
);
CREATE INDEX idx_employee_stores_store ON employee_stores (store_id);

-- Carry over each employee's current store.
INSERT INTO employee_stores (employee_id, store_id)
SELECT id, store_id FROM employees WHERE store_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Drop the old single-store column (also drops its foreign key).
ALTER TABLE employees DROP COLUMN store_id;

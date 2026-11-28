-- Annual budget per company (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1, OR-01).
--
-- One budget per company and year; one line per account (classes 6/7) x cost centre x month, amount >= 0.
CREATE TABLE IF NOT EXISTS budgets (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id),
    fiscal_year INTEGER      NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    CONSTRAINT uk_budgets_company_year UNIQUE (company_id, fiscal_year)
);

CREATE TABLE IF NOT EXISTS budget_lines (
    id             BIGSERIAL PRIMARY KEY,
    budget_id      BIGINT         NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    account_id     BIGINT         NOT NULL REFERENCES accounts(id),
    cost_center_id BIGINT         NOT NULL REFERENCES cost_centers(id),
    budget_month   INTEGER        NOT NULL,
    amount         NUMERIC(16, 2) NOT NULL,
    CONSTRAINT uk_budget_lines_cell UNIQUE (budget_id, account_id, cost_center_id, budget_month),
    CONSTRAINT budget_lines_month_check CHECK (budget_month BETWEEN 1 AND 12),
    CONSTRAINT budget_lines_amount_check CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_budget_lines_cost_center ON budget_lines (cost_center_id);

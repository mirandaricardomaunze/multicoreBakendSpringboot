-- Budget alerts per cost centre and month (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.3, OR-05).
--
-- One alert per company x cost centre x year x month x threshold (90 or 100): never repeated.
CREATE TABLE IF NOT EXISTS budget_alerts (
    id                BIGSERIAL PRIMARY KEY,
    company_id        BIGINT         NOT NULL REFERENCES companies(id),
    cost_center_id    BIGINT         NOT NULL REFERENCES cost_centers(id),
    fiscal_year       INTEGER        NOT NULL,
    alert_month       INTEGER        NOT NULL,
    threshold_percent INTEGER        NOT NULL,
    budgeted_amount   NUMERIC(16, 2) NOT NULL,
    actual_amount     NUMERIC(16, 2) NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    CONSTRAINT uk_budget_alerts_threshold UNIQUE (company_id, cost_center_id, fiscal_year, alert_month, threshold_percent),
    CONSTRAINT budget_alerts_month_check CHECK (alert_month BETWEEN 1 AND 12),
    CONSTRAINT budget_alerts_threshold_check CHECK (threshold_percent IN (90, 100))
);

CREATE INDEX IF NOT EXISTS idx_budget_alerts_company_year ON budget_alerts (company_id, fiscal_year);

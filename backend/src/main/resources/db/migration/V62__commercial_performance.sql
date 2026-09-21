-- V62: Centro de Desempenho Comercial (CDC) - Metas, Prémios e Integração com Folha Salarial

ALTER TABLE payslips
    ADD COLUMN IF NOT EXISTS sales_bonus NUMERIC(14, 2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS sales_goals (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    period VARCHAR(32) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    scope VARCHAR(32) NOT NULL,
    scope_ref_id BIGINT,
    scope_label VARCHAR(255),
    target_revenue NUMERIC(14, 2),
    target_margin NUMERIC(14, 2),
    target_margin_pct NUMERIC(7, 4),
    bonus_type VARCHAR(32) NOT NULL,
    bonus_value NUMERIC(14, 2) NOT NULL,
    bonus_cap NUMERIC(14, 2),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    auto_apply_bonus BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    CONSTRAINT uk_sales_goals_company_name_start UNIQUE (company_id, name, period_start)
);

CREATE INDEX IF NOT EXISTS idx_sales_goals_company_status ON sales_goals (company_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_goals_scope ON sales_goals (company_id, scope, scope_ref_id);

CREATE TABLE IF NOT EXISTS sales_goal_bonuses (
    id BIGSERIAL PRIMARY KEY,
    goal_id BIGINT NOT NULL REFERENCES sales_goals(id),
    employee_id BIGINT REFERENCES employees(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    calculated_amount NUMERIC(14, 2) NOT NULL,
    approved_amount NUMERIC(14, 2) NOT NULL,
    justification VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    payslip_id BIGINT REFERENCES payslips(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    approved_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_sales_goal_bonuses_company_status ON sales_goal_bonuses (company_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_goal_bonuses_goal ON sales_goal_bonuses (goal_id);
CREATE INDEX IF NOT EXISTS idx_sales_goal_bonuses_employee ON sales_goal_bonuses (employee_id);

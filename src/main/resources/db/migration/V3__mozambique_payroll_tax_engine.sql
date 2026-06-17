ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS dependents_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS employer_inss NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS taxable_income NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS irps_rate NUMERIC(7,4) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS employee_inss_rate NUMERIC(7,4) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS employer_inss_rate NUMERIC(7,4) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS tax_config_name VARCHAR(255);
ALTER TABLE IF EXISTS payslips ADD COLUMN IF NOT EXISTS tax_legal_basis VARCHAR(500);

CREATE TABLE IF NOT EXISTS payroll_tax_configs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    employee_inss_rate NUMERIC(7,4) NOT NULL,
    employer_inss_rate NUMERIC(7,4) NOT NULL,
    legal_basis VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS payroll_irps_brackets (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES payroll_tax_configs(id) ON DELETE CASCADE,
    lower_bound NUMERIC(14,2) NOT NULL,
    upper_bound NUMERIC(14,2),
    rate NUMERIC(7,4) NOT NULL,
    fixed_deduction NUMERIC(14,2) NOT NULL DEFAULT 0,
    dependent_deduction NUMERIC(14,2) NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_payroll_tax_config_company_period
    ON payroll_tax_configs(company_id, effective_from, effective_to, active);

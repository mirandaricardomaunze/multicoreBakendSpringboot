-- =========================================================================
-- V85: Módulo de Contratos & Facturação Recorrente (Recurring Billing)
-- Gestão de avenças, contratos de prestação de serviços e facturação periódica
-- =========================================================================

CREATE TABLE IF NOT EXISTS recurring_contracts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    contract_number VARCHAR(32) NOT NULL,
    description VARCHAR(250) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    billing_day_of_month INT NOT NULL DEFAULT 1,
    fixed_amount NUMERIC(15, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_billing_date DATE NOT NULL,
    last_billed_date DATE,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_company_contract_number UNIQUE (company_id, contract_number)
);

CREATE TABLE IF NOT EXISTS recurring_billing_logs (
    id BIGSERIAL PRIMARY KEY,
    contract_id BIGINT NOT NULL REFERENCES recurring_contracts(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE SET NULL,
    billing_date DATE NOT NULL,
    billed_amount NUMERIC(15, 2) NOT NULL,
    invoice_number VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    notes VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recurring_contracts_company ON recurring_contracts(company_id);
CREATE INDEX IF NOT EXISTS idx_recurring_contracts_client ON recurring_contracts(client_id);
CREATE INDEX IF NOT EXISTS idx_recurring_contracts_next_billing ON recurring_contracts(company_id, status, next_billing_date);
CREATE INDEX IF NOT EXISTS idx_recurring_billing_logs_contract ON recurring_billing_logs(contract_id);

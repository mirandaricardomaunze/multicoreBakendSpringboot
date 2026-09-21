-- V64: Centro de Reconciliação Bancária & Importação de Extractos (Bank Statement Reconciliation)

CREATE TABLE IF NOT EXISTS bank_statements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    treasury_account_id BIGINT NOT NULL REFERENCES treasury_accounts(id),
    statement_reference VARCHAR(80) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    opening_balance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    closing_balance NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_bank_statements_company ON bank_statements (company_id);
CREATE INDEX IF NOT EXISTS idx_bank_statements_account ON bank_statements (treasury_account_id);
CREATE INDEX IF NOT EXISTS idx_bank_statements_status ON bank_statements (company_id, status);

CREATE TABLE IF NOT EXISTS bank_statement_items (
    id BIGSERIAL PRIMARY KEY,
    bank_statement_id BIGINT NOT NULL REFERENCES bank_statements(id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    value_date DATE,
    description VARCHAR(255) NOT NULL,
    reference VARCHAR(100),
    amount NUMERIC(14, 2) NOT NULL,
    balance_after NUMERIC(14, 2),
    status VARCHAR(30) NOT NULL DEFAULT 'UNMATCHED',
    matched_transaction_id BIGINT REFERENCES treasury_transactions(id),
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_statement_items_statement ON bank_statement_items (bank_statement_id);
CREATE INDEX IF NOT EXISTS idx_statement_items_status ON bank_statement_items (status);
CREATE INDEX IF NOT EXISTS idx_statement_items_matched_tx ON bank_statement_items (matched_transaction_id);

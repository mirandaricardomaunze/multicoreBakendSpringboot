-- V70__mobile_payment_transactions.sql
-- Tabela para rastreabilidade de transações de pagamento móvel Push USSD (M-Pesa e e-Mola)

CREATE TABLE IF NOT EXISTS mobile_payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    provider VARCHAR(20) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    reference VARCHAR(64),
    financial_reference VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    message VARCHAR(255),
    operator VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mobile_payment_tx_id ON mobile_payment_transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_mobile_payment_company ON mobile_payment_transactions(company_id);

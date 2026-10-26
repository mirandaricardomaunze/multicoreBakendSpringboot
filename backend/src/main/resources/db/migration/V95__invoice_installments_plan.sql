-- =========================================================================
-- V95: Vencimentos Fracionados e Prestações Comerciais (Multi-Installments)
-- Divisão de faturas em parcelas escalonadas com controle de saldo individual
-- =========================================================================

CREATE TABLE IF NOT EXISTS invoice_installments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    total_installments INT NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    paid_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    remaining_balance NUMERIC(14, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_invoice_installment UNIQUE (invoice_id, installment_number)
);

CREATE INDEX IF NOT EXISTS idx_invoice_installments_invoice ON invoice_installments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_installments_company_due ON invoice_installments(company_id, due_date ASC);
CREATE INDEX IF NOT EXISTS idx_invoice_installments_status ON invoice_installments(company_id, status);

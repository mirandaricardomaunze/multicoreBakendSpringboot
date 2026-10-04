-- Adiciona invoice_id e invoice_number à tabela quotations para rastreio de conversão directa em factura ou POS
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS invoice_id BIGINT REFERENCES invoices(id);
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_quotations_company_invoice ON quotations (company_id, invoice_id);

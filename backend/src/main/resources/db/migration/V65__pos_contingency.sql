-- Suporte ao Modo de Contingência / Offline-First do POS
ALTER TABLE invoices ADD COLUMN contingency_reference VARCHAR(60);
CREATE INDEX idx_invoices_contingency ON invoices(company_id, contingency_reference);

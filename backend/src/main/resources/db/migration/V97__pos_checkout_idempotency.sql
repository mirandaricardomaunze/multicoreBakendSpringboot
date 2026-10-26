ALTER TABLE invoices ADD COLUMN IF NOT EXISTS pos_request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_invoices_company_contingency_reference
    ON invoices(company_id, contingency_reference);

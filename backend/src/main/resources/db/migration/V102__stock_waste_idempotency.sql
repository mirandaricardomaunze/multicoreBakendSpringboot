ALTER TABLE stock_waste_records ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(60);
ALTER TABLE stock_waste_records ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_waste_company_idempotency
    ON stock_waste_records(company_id, idempotency_key);

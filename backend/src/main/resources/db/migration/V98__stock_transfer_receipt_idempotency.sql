ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS receipt_idempotency_key VARCHAR(60);
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS receipt_request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_transfer_receipt_idempotency
    ON stock_transfers(company_id, receipt_idempotency_key);

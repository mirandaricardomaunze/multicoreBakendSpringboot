ALTER TABLE payment_entries ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(60);
ALTER TABLE payment_entries ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_entries_invoice_idempotency
    ON payment_entries(invoice_id, idempotency_key);

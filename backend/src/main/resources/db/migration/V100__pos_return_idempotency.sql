ALTER TABLE credit_notes ADD COLUMN IF NOT EXISTS pos_return_idempotency_key VARCHAR(60);
ALTER TABLE credit_notes ADD COLUMN IF NOT EXISTS pos_return_request_fingerprint VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_credit_notes_invoice_pos_return
    ON credit_notes(invoice_id, pos_return_idempotency_key);

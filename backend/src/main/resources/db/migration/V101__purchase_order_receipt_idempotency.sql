CREATE TABLE IF NOT EXISTS purchase_order_receipts (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id),
    idempotency_key VARCHAR(60) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uk_purchase_order_receipt_key UNIQUE (purchase_order_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_purchase_order_receipts_order
    ON purchase_order_receipts(purchase_order_id);

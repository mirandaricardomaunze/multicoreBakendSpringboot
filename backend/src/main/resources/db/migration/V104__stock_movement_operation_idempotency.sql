CREATE TABLE IF NOT EXISTS stock_movement_operations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    stock_movement_id BIGINT REFERENCES stock_movements(id),
    idempotency_key VARCHAR(60) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT uk_stock_movement_operation_company_key UNIQUE (company_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_stock_movement_operation_movement
    ON stock_movement_operations(stock_movement_id);

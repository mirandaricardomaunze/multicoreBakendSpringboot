CREATE TABLE IF NOT EXISTS stock_adjustment_operations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    stock_movement_id BIGINT REFERENCES stock_movements(id),
    idempotency_key VARCHAR(60) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT uk_stock_adjustment_company_key UNIQUE (company_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_stock_adjustment_movement
    ON stock_adjustment_operations(stock_movement_id);

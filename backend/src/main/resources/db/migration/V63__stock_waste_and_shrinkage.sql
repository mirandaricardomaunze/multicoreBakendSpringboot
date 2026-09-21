-- V63: Centro de Gestão de Quebras, Perdas e Prevenção de Desperdício (Waste & Shrinkage Management)

CREATE TABLE IF NOT EXISTS stock_waste_records (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    batch_id BIGINT REFERENCES product_batches(id),
    quantity NUMERIC(14, 3) NOT NULL,
    unit_cost NUMERIC(14, 2) NOT NULL,
    total_cost NUMERIC(14, 2) NOT NULL,
    reason VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
    notes VARCHAR(500),
    registered_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    stock_movement_id BIGINT REFERENCES stock_movements(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_stock_waste_company_status ON stock_waste_records (company_id, status);
CREATE INDEX IF NOT EXISTS idx_stock_waste_company_created ON stock_waste_records (company_id, created_at);
CREATE INDEX IF NOT EXISTS idx_stock_waste_product ON stock_waste_records (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_waste_warehouse ON stock_waste_records (warehouse_id);

-- Migração V66: Inventário & Contagem Física de Stock
CREATE TABLE IF NOT EXISTS inventory_physical_sessions (
    id BIGSERIAL PRIMARY KEY,
    inventory_number VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    blind_counting BOOLEAN NOT NULL DEFAULT FALSE,
    start_date TIMESTAMP(6) NOT NULL,
    end_date TIMESTAMP(6),
    company_id BIGINT NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    items_counted INT NOT NULL DEFAULT 0,
    total_surplus_value DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_deficit_value DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    net_financial_impact DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT idx_inv_session_comp_num UNIQUE (company_id, inventory_number)
);

CREATE TABLE IF NOT EXISTS inventory_physical_items (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(255),
    barcode VARCHAR(100),
    expected_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    counted_quantity DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    difference DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    unit_cost DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    financial_impact DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT fk_inv_item_session FOREIGN KEY (session_id) REFERENCES inventory_physical_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE INDEX idx_inv_session_company ON inventory_physical_sessions(company_id, status);
CREATE INDEX idx_inv_item_session ON inventory_physical_items(session_id);
CREATE INDEX idx_inv_item_product ON inventory_physical_items(product_id);

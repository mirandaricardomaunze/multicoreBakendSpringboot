-- =========================================================================
-- V83: Módulo de Produção & Transformação Industrial (Manufacturing & BOM)
-- Fichas Técnicas (Bill of Materials) e Ordens de Produção (OP)
-- =========================================================================

CREATE TABLE IF NOT EXISTS manufacturing_boms (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    bom_code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    yield_quantity NUMERIC(15, 4) NOT NULL DEFAULT 1.0,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_company_bom_code UNIQUE (company_id, bom_code)
);

CREATE TABLE IF NOT EXISTS manufacturing_bom_items (
    id BIGSERIAL PRIMARY KEY,
    bom_id BIGINT NOT NULL REFERENCES manufacturing_boms(id) ON DELETE CASCADE,
    component_product_id BIGINT NOT NULL REFERENCES products(id),
    quantity_required NUMERIC(15, 4) NOT NULL,
    unit_of_measure VARCHAR(30),
    scrap_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.0
);

CREATE INDEX IF NOT EXISTS idx_bom_company ON manufacturing_boms(company_id);
CREATE INDEX IF NOT EXISTS idx_bom_product ON manufacturing_boms(product_id);
CREATE INDEX IF NOT EXISTS idx_bom_items_bom ON manufacturing_bom_items(bom_id);

CREATE TABLE IF NOT EXISTS production_orders (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    bom_id BIGINT NOT NULL REFERENCES manufacturing_boms(id),
    order_number VARCHAR(50) NOT NULL,
    target_product_id BIGINT NOT NULL REFERENCES products(id),
    quantity_to_produce NUMERIC(15, 4) NOT NULL,
    produced_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    planned_start_date DATE NOT NULL,
    actual_start_date DATE,
    completion_date DATE,
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    total_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_company_production_order_number UNIQUE (company_id, order_number)
);

CREATE TABLE IF NOT EXISTS production_order_lines (
    id BIGSERIAL PRIMARY KEY,
    production_order_id BIGINT NOT NULL REFERENCES production_orders(id) ON DELETE CASCADE,
    component_product_id BIGINT NOT NULL REFERENCES products(id),
    planned_quantity NUMERIC(15, 4) NOT NULL,
    consumed_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0,
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    total_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.0
);

CREATE INDEX IF NOT EXISTS idx_production_orders_company ON production_orders(company_id);
CREATE INDEX IF NOT EXISTS idx_production_orders_status ON production_orders(company_id, status);
CREATE INDEX IF NOT EXISTS idx_production_order_lines_order ON production_order_lines(production_order_id);

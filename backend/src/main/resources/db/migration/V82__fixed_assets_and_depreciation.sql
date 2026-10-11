-- V82: Gestão de Activos Fixos, Imobilizado e Amortizações PGC-NIRF / IRPC

CREATE TABLE IF NOT EXISTS fixed_assets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    asset_code VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    acquisition_date DATE NOT NULL,
    acquisition_cost NUMERIC(14, 2) NOT NULL,
    residual_value NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    annual_depreciation_rate NUMERIC(6, 4) NOT NULL,
    accumulated_depreciation NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    net_book_value NUMERIC(14, 2) NOT NULL,
    serial_number VARCHAR(100),
    location VARCHAR(120),
    assigned_employee VARCHAR(150),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT uk_fixed_assets_code UNIQUE (company_id, asset_code)
);

CREATE INDEX IF NOT EXISTS idx_fixed_assets_company ON fixed_assets (company_id);
CREATE INDEX IF NOT EXISTS idx_fixed_assets_category ON fixed_assets (company_id, category);
CREATE INDEX IF NOT EXISTS idx_fixed_assets_status ON fixed_assets (company_id, status);

CREATE TABLE IF NOT EXISTS asset_depreciation_records (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    asset_id BIGINT NOT NULL REFERENCES fixed_assets(id) ON DELETE CASCADE,
    period_year INT NOT NULL,
    period_month INT NOT NULL,
    calculation_date DATE NOT NULL,
    opening_book_value NUMERIC(14, 2) NOT NULL,
    depreciation_amount NUMERIC(14, 2) NOT NULL,
    closing_book_value NUMERIC(14, 2) NOT NULL,
    accumulated_depreciation NUMERIC(14, 2) NOT NULL,
    journal_entry_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT uk_asset_depreciation_period UNIQUE (company_id, asset_id, period_year, period_month)
);

CREATE INDEX IF NOT EXISTS idx_depreciation_records_asset ON asset_depreciation_records (asset_id);
CREATE INDEX IF NOT EXISTS idx_depreciation_records_period ON asset_depreciation_records (company_id, period_year, period_month);

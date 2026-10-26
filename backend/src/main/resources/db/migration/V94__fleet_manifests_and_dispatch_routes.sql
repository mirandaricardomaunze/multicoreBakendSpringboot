-- =========================================================================
-- V94: Manifesto de Carga e Gestão de Rotas de Distribuição
-- Agrupamento de Guias de Remessa / Encomendas por Viatura, Motorista e Rota
-- =========================================================================

CREATE TABLE IF NOT EXISTS fleet_manifests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    manifest_number VARCHAR(50) NOT NULL,
    manifest_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(30) NOT NULL DEFAULT 'IN_TRANSIT',
    vehicle_id BIGINT REFERENCES fleet_vehicles(id) ON DELETE SET NULL,
    vehicle_plate VARCHAR(20) NOT NULL,
    vehicle_model VARCHAR(100),
    driver_name VARCHAR(150) NOT NULL,
    driver_phone VARCHAR(50),
    route_name VARCHAR(150) NOT NULL,
    departure_time VARCHAR(50),
    notes TEXT,
    total_stops INT NOT NULL DEFAULT 0,
    total_boxes NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_packages NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount_to_collect NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_company_manifest_number UNIQUE (company_id, manifest_number)
);

CREATE TABLE IF NOT EXISTS fleet_manifest_stops (
    id BIGSERIAL PRIMARY KEY,
    manifest_id BIGINT NOT NULL REFERENCES fleet_manifests(id) ON DELETE CASCADE,
    sequence INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_id BIGINT,
    document_number VARCHAR(50) NOT NULL,
    client_id BIGINT REFERENCES clients(id) ON DELETE SET NULL,
    client_name VARCHAR(200) NOT NULL,
    delivery_address TEXT,
    contact_phone VARCHAR(50),
    box_count NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    package_count NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    amount_to_collect NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    payment_terms VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fleet_manifests_company ON fleet_manifests(company_id);
CREATE INDEX IF NOT EXISTS idx_fleet_manifests_date ON fleet_manifests(company_id, manifest_date DESC);
CREATE INDEX IF NOT EXISTS idx_fleet_manifest_stops_manifest ON fleet_manifest_stops(manifest_id);

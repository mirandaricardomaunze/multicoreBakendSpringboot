-- =========================================================================
-- V84: Módulo de Gestão de Frotas & Viaturas (Fleet Management)
-- Viaturas, quilometragem, histórico de abastecimentos e controlo Km/L
-- =========================================================================

CREATE TABLE IF NOT EXISTS fleet_vehicles (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    license_plate VARCHAR(20) NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    manufacturing_year INT NOT NULL,
    fuel_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    current_odometer_km NUMERIC(15, 2) NOT NULL DEFAULT 0.0,
    assigned_driver_name VARCHAR(150),
    insurance_expiry_date DATE,
    inspection_expiry_date DATE,
    avg_consumption_km_per_liter NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_company_license_plate UNIQUE (company_id, license_plate)
);

CREATE TABLE IF NOT EXISTS fleet_fuel_logs (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id BIGINT NOT NULL REFERENCES fleet_vehicles(id) ON DELETE CASCADE,
    log_timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    odometer_km NUMERIC(15, 2) NOT NULL,
    liters_filled NUMERIC(12, 2) NOT NULL,
    price_per_liter NUMERIC(12, 2) NOT NULL,
    total_cost NUMERIC(15, 2) NOT NULL,
    gas_station_name VARCHAR(150),
    driver_name VARCHAR(150),
    distance_traveled_km NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    consumption_rate_km_per_liter NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fleet_vehicles_company ON fleet_vehicles(company_id);
CREATE INDEX IF NOT EXISTS idx_fleet_vehicles_plate ON fleet_vehicles(company_id, license_plate);
CREATE INDEX IF NOT EXISTS idx_fleet_fuel_logs_vehicle ON fleet_fuel_logs(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_fleet_fuel_logs_timestamp ON fleet_fuel_logs(vehicle_id, log_timestamp DESC);

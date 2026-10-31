-- V105: Paridade do schema PostgreSQL com as entidades JPA.
-- As migrações divergiram porque o desenvolvimento corre em H2 com ddl-auto=update.
-- Ver docs/POSTGRES_SCHEMA_PARITY_SPEC.md.

-- Utilizadores
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS manager_pin_hash VARCHAR(255);

-- Produção
ALTER TABLE manufacturing_boms ADD COLUMN IF NOT EXISTS bom_type VARCHAR(30);
ALTER TABLE manufacturing_boms ADD COLUMN IF NOT EXISTS estimated_labor_cost NUMERIC(15, 2);
ALTER TABLE manufacturing_boms ADD COLUMN IF NOT EXISTS estimated_overhead_cost NUMERIC(15, 2);
ALTER TABLE production_orders ADD COLUMN IF NOT EXISTS planned_end_date DATE;
ALTER TABLE production_orders ADD COLUMN IF NOT EXISTS source_warehouse_id BIGINT;
ALTER TABLE production_orders ADD COLUMN IF NOT EXISTS target_warehouse_id BIGINT;
ALTER TABLE production_orders ADD CONSTRAINT fk_production_order_source_warehouse
    FOREIGN KEY (source_warehouse_id) REFERENCES warehouses(id);
ALTER TABLE production_orders ADD CONSTRAINT fk_production_order_target_warehouse
    FOREIGN KEY (target_warehouse_id) REFERENCES warehouses(id);

-- Campos herdados de BaseEntity
ALTER TABLE business_units ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE fleet_manifest_stops ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE fleet_manifests ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE fleet_vehicles ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE inventory_physical_items ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE inventory_physical_sessions ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE invoice_installments ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE manufacturing_boms ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE mobile_payment_transactions ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE production_orders ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE recurring_billing_logs ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE recurring_contracts ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE fleet_manifest_stops ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);
ALTER TABLE recurring_billing_logs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);

-- BaseEntity só preenche updated_at em @PreUpdate: NOT NULL faria falhar todos os inserts
ALTER TABLE document_print_controls ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE fleet_manifests ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE fleet_vehicles ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE invoice_installments ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE manufacturing_boms ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE mobile_payment_transactions ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE production_orders ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE recurring_contracts ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE store_vouchers ALTER COLUMN updated_at DROP NOT NULL;

-- A entidade usa LocalDateTime
ALTER TABLE mobile_payment_transactions ALTER COLUMN created_at TYPE TIMESTAMP(6);
ALTER TABLE mobile_payment_transactions ALTER COLUMN updated_at TYPE TIMESTAMP(6);

-- Os CHECKs antigos rejeitavam M-Pesa, e-Mola, crédito de loja, abates e reembolsos de caixa
ALTER TABLE payment_entries DROP CONSTRAINT IF EXISTS payment_entries_method_check;
ALTER TABLE payment_entries ADD CONSTRAINT payment_entries_method_check
    CHECK (method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'MPESA', 'EMOLA', 'CREDIT', 'STORE_CREDIT'));
ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_movement_type_check;
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_movement_type_check
    CHECK (movement_type IN ('PURCHASE', 'ENTRY', 'SALE', 'TRANSFER', 'ADJUSTMENT', 'RETURN', 'REVERSAL', 'WASTE'));
ALTER TABLE till_movements DROP CONSTRAINT IF EXISTS till_movements_movement_type_check;
ALTER TABLE till_movements ADD CONSTRAINT till_movements_movement_type_check
    CHECK (movement_type IN ('SALE', 'SUPRIMENTO', 'SANGRIA', 'REFUND'));

-- Uma operação de ajuste corresponde a um único movimento de stock
ALTER TABLE stock_adjustment_operations ADD CONSTRAINT uk_stock_adjustment_operation_movement
    UNIQUE (stock_movement_id);

-- Módulo de qualidade: tabelas que nenhuma migração criava
CREATE TABLE IF NOT EXISTS quality_equipments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6),
    created_by VARCHAR(255),
    company_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(40) NOT NULL
        CHECK (category IN ('PPE_PROTECTION', 'CALIBRATION_TOOL', 'SAFETY_DEVICE', 'WORK_TOOL')),
    period_unit VARCHAR(20) NOT NULL CHECK (period_unit IN ('DAYS', 'MONTHS')),
    period_value INTEGER NOT NULL,
    warning_days_before INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    CONSTRAINT uk_quality_equipment_code UNIQUE (company_id, code),
    CONSTRAINT fk_quality_equipment_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE IF NOT EXISTS quality_worker_assignments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6),
    created_by VARCHAR(255),
    company_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    equipment_id BIGINT NOT NULL,
    assigned_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'RENEWED', 'RETURNED', 'DISCARDED')),
    size_or_spec VARCHAR(60),
    serial_number VARCHAR(80),
    notes VARCHAR(500),
    CONSTRAINT fk_qwa_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_qwa_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_qwa_equipment FOREIGN KEY (equipment_id) REFERENCES quality_equipments(id)
);

CREATE INDEX IF NOT EXISTS idx_qwa_company_status ON quality_worker_assignments(company_id, status);
CREATE INDEX IF NOT EXISTS idx_qwa_employee ON quality_worker_assignments(company_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_qwa_expiry ON quality_worker_assignments(company_id, expiry_date);

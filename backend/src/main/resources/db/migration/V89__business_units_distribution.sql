-- V89: Módulo de Gestão de Negócios e Linhas de Distribuição
-- Permite categorizar produtos por unidades de negócio (ex.: Unilever, BAT, Food)
-- e filtrar compras, stocks e documentos comerciais.

CREATE TABLE IF NOT EXISTS business_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    company_id BIGINT NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    supplier_id BIGINT,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_business_unit_company_code UNIQUE (company_id, code),
    CONSTRAINT fk_business_unit_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_business_unit_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

ALTER TABLE products ADD COLUMN IF NOT EXISTS business_unit_id BIGINT;
ALTER TABLE products ADD CONSTRAINT fk_product_business_unit FOREIGN KEY (business_unit_id) REFERENCES business_units(id);

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS business_unit_id BIGINT;
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_business_unit FOREIGN KEY (business_unit_id) REFERENCES business_units(id);

ALTER TABLE orders ADD COLUMN IF NOT EXISTS business_unit_id BIGINT;
ALTER TABLE orders ADD CONSTRAINT fk_order_business_unit FOREIGN KEY (business_unit_id) REFERENCES business_units(id);

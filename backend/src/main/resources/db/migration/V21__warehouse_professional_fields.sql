-- Campos profissionais do armazém: activo, tipo, permite vendas (POS), responsável, telefone.
-- Existentes ficam activos, tipo Loja e a permitir vendas (comportamento anterior preservado).
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS active       boolean      NOT NULL DEFAULT TRUE;
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS type         varchar(20)  DEFAULT 'STORE';
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS allows_sales boolean      NOT NULL DEFAULT TRUE;
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS manager      varchar(255);
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS phone        varchar(255);

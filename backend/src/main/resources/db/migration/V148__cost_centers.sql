-- Centros de custo por empresa (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-01).
--
-- Cada armazém/loja tem o seu centro (type WAREHOUSE, warehouse_id preenchido) e o gestor cria centros manuais.
-- As linhas de lançamento passam a poder indicar o centro; um centro com linhas não se apaga.
CREATE TABLE IF NOT EXISTS cost_centers (
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT       NOT NULL REFERENCES companies(id),
    code         VARCHAR(20)  NOT NULL,
    name         VARCHAR(120) NOT NULL,
    type         VARCHAR(20)  NOT NULL,
    warehouse_id BIGINT       REFERENCES warehouses(id),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    CONSTRAINT uk_cost_centers_company_code UNIQUE (company_id, code),
    CONSTRAINT cost_centers_type_check CHECK (type IN ('WAREHOUSE', 'MANUAL'))
);

CREATE INDEX IF NOT EXISTS idx_cost_centers_warehouse ON cost_centers (warehouse_id);

ALTER TABLE journal_lines ADD COLUMN IF NOT EXISTS cost_center_id BIGINT REFERENCES cost_centers(id);

CREATE INDEX IF NOT EXISTS idx_journal_lines_cost_center ON journal_lines (cost_center_id);

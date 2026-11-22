-- Efeito do documento "Custos de Importação" em cada linha de compra (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.4,
-- LC-06), em moeda base: quota repartida, quantidade recebida, stock do artigo no armazém na confirmação, parte que
-- subiu o custo médio, parte já vendida (CMV 6101) e o custo médio antes e depois. Fotografia da confirmação.
CREATE TABLE IF NOT EXISTS landed_cost_allocations (
    id                      BIGSERIAL PRIMARY KEY,
    landed_cost_document_id BIGINT         NOT NULL REFERENCES landed_cost_documents(id),
    sort_order              INTEGER,
    purchase_line_id        BIGINT         NOT NULL REFERENCES purchase_lines(id),
    received_quantity       NUMERIC(12, 3) NOT NULL,
    stock_quantity          NUMERIC(12, 3) NOT NULL,
    share_amount            NUMERIC(14, 2) NOT NULL,
    stock_amount            NUMERIC(14, 2) NOT NULL,
    sold_amount             NUMERIC(14, 2) NOT NULL,
    unit_cost_before        NUMERIC(14, 2) NOT NULL,
    unit_cost_after         NUMERIC(14, 2) NOT NULL,
    created_at              TIMESTAMP      NOT NULL,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_landed_cost_allocations_document ON landed_cost_allocations (landed_cost_document_id);

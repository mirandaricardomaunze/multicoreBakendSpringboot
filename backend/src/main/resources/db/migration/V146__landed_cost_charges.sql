-- Linhas de custo do documento "Custos de Importação" (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1, LC-02).
--
-- Cada linha tem tipo, fornecedor, moeda e câmbio (fotografia), valor sem IVA na moeda da linha e a taxa de IVA do
-- serviço em percentagem. A dívida de cada linha ao seu fornecedor fica em purchases com document_kind
-- 'LANDED_COST' (sem armazém nem linhas), para aparecer nas contas a pagar e se pagar como uma compra.
CREATE TABLE IF NOT EXISTS landed_cost_charges (
    id                      BIGSERIAL PRIMARY KEY,
    landed_cost_document_id BIGINT         NOT NULL REFERENCES landed_cost_documents(id),
    sort_order              INTEGER,
    charge_type             VARCHAR(30)    NOT NULL,
    supplier_id             BIGINT         NOT NULL REFERENCES suppliers(id),
    currency                VARCHAR(3)     NOT NULL,
    exchange_rate           NUMERIC(18, 4) NOT NULL,
    amount                  NUMERIC(14, 2) NOT NULL,
    vat_rate_percent        NUMERIC(7, 4),
    payable_purchase_id     BIGINT         NOT NULL REFERENCES purchases(id),
    created_at              TIMESTAMP      NOT NULL,
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255),
    CONSTRAINT uk_landed_cost_charges_payable UNIQUE (payable_purchase_id),
    CONSTRAINT landed_cost_charges_charge_type_check CHECK (charge_type IN
        ('FRETE', 'SEGURO', 'DIREITOS_ADUANEIROS', 'DESPACHANTE', 'IVA_ADUANEIRO', 'OUTRO')),
    CONSTRAINT landed_cost_charges_amount_check CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_landed_cost_charges_document ON landed_cost_charges (landed_cost_document_id);

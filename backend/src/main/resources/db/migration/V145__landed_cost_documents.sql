-- "Custos de Importação" documents, series CI (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1, LC-01).
--
-- One document per company and number; it links to one or more received purchases of the same company, and a
-- purchase may be linked to several documents. "Only received purchases of the company" lives in the service.
CREATE TABLE IF NOT EXISTS landed_cost_documents (
    id                BIGSERIAL PRIMARY KEY,
    company_id        BIGINT       NOT NULL REFERENCES companies(id),
    document_number   VARCHAR(40)  NOT NULL,
    document_date     DATE         NOT NULL,
    allocation_method VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    CONSTRAINT uk_landed_cost_documents_company_number UNIQUE (company_id, document_number),
    CONSTRAINT landed_cost_documents_allocation_method_check
        CHECK (allocation_method IN ('VALOR', 'PESO', 'QUANTIDADE'))
);

CREATE TABLE IF NOT EXISTS landed_cost_document_purchases (
    landed_cost_document_id BIGINT  NOT NULL REFERENCES landed_cost_documents(id),
    purchase_id             BIGINT  NOT NULL REFERENCES purchases(id),
    sort_order              INTEGER NOT NULL,
    PRIMARY KEY (landed_cost_document_id, sort_order)
);

CREATE INDEX IF NOT EXISTS idx_landed_cost_document_purchases_purchase
    ON landed_cost_document_purchases (purchase_id);

-- Return of goods to a supplier (internal series DF): lowers the debt, the stock and the deductible VAT.
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS returned_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE purchase_lines ADD COLUMN IF NOT EXISTS returned_quantity NUMERIC(12, 3) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS supplier_returns (
    id            BIGSERIAL PRIMARY KEY,
    return_number VARCHAR(40)    NOT NULL,
    company_id    BIGINT         NOT NULL REFERENCES companies(id),
    purchase_id   BIGINT         NOT NULL REFERENCES purchases(id),
    return_date   TIMESTAMP      NOT NULL,
    net_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    tax_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    reason        VARCHAR(300)   NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    CONSTRAINT uk_supplier_returns_company_number UNIQUE (company_id, return_number)
);
CREATE INDEX IF NOT EXISTS idx_supplier_returns_purchase ON supplier_returns(purchase_id);

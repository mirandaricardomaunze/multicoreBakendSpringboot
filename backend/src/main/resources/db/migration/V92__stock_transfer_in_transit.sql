-- Stock em trânsito (transferência em 2 passos). Ver docs/STOCK_IN_TRANSIT_SPEC.md.
-- Opt-in por guia: requires_receipt = FALSE mantém o fluxo de 1 passo existente.
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS requires_receipt BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMP;
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS dispatched_by VARCHAR(100);
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS received_at TIMESTAMP;
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS received_by VARCHAR(100);
ALTER TABLE stock_transfers ADD COLUMN IF NOT EXISTS receipt_notes VARCHAR(500);

-- Lotes que saíram da origem e estão em viagem: é o que a recepção credita no destino.
CREATE TABLE IF NOT EXISTS stock_transfer_line_batches (
    id BIGSERIAL PRIMARY KEY,
    transfer_line_id BIGINT NOT NULL REFERENCES stock_transfer_lines (id) ON DELETE CASCADE,
    batch_number VARCHAR(120) NOT NULL,
    expiration_date DATE,
    quantity NUMERIC(14,3) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stl_batches_line ON stock_transfer_line_batches (transfer_line_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_company_status ON stock_transfers (company_id, status);

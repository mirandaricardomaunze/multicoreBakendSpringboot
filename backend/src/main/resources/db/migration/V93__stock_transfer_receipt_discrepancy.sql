-- Recepção de guias em trânsito com divergências. Ver docs/STOCK_TRANSFER_RECEIPT_DISCREPANCY_SPEC.md.
-- Aditiva e não destrutiva: colunas nulas para guias já recebidas antes desta versão.
ALTER TABLE stock_transfer_lines ADD COLUMN IF NOT EXISTS received_quantity NUMERIC(12,3);
ALTER TABLE stock_transfer_lines ADD COLUMN IF NOT EXISTS returned_quantity NUMERIC(12,3);
ALTER TABLE stock_transfer_lines ADD COLUMN IF NOT EXISTS discrepancy_reason VARCHAR(200);

-- Parte de cada lote em viagem que entrou no destino (o restante voltou à origem).
ALTER TABLE stock_transfer_line_batches ADD COLUMN IF NOT EXISTS received_quantity NUMERIC(14,3);

-- Concorrência optimista no editor de encomendas a fornecedor.
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

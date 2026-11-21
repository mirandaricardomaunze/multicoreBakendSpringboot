-- Moeda e câmbio nas encomendas e compras (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3, FX-05).
--
-- O documento guarda a moeda e o câmbio usado (fotografia, sem ligação à tabela de câmbios). Os documentos
-- existentes ficam na moeda base da sua empresa com câmbio 1. Pode correr outra vez sem efeito.
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'MZN' NOT NULL;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18,4) DEFAULT 1 NOT NULL;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'MZN' NOT NULL;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS exchange_rate NUMERIC(18,4) DEFAULT 1 NOT NULL;

UPDATE purchase_orders SET currency = (SELECT c.base_currency FROM companies c WHERE c.id = purchase_orders.company_id)
WHERE exchange_rate = 1
  AND currency <> (SELECT c.base_currency FROM companies c WHERE c.id = purchase_orders.company_id);
UPDATE purchases SET currency = (SELECT c.base_currency FROM companies c WHERE c.id = purchases.company_id)
WHERE exchange_rate = 1
  AND currency <> (SELECT c.base_currency FROM companies c WHERE c.id = purchases.company_id);

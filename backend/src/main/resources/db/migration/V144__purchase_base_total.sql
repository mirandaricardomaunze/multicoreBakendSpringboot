-- Total em moeda base nas encomendas e compras (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3, FX-06).
--
-- O documento guarda o total em moeda base = total × câmbio, arredondado a 2 casas (HALF_UP), como fotografia.
-- Os documentos existentes recebem-no do total e do câmbio que já têm. Pode correr outra vez sem efeito.
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS base_total_amount NUMERIC(14,2) DEFAULT 0 NOT NULL;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS base_total_amount NUMERIC(14,2) DEFAULT 0 NOT NULL;

UPDATE purchase_orders SET base_total_amount = ROUND(total_amount * exchange_rate, 2)
WHERE base_total_amount <> ROUND(total_amount * exchange_rate, 2);
UPDATE purchases SET base_total_amount = ROUND(total_amount * exchange_rate, 2)
WHERE base_total_amount <> ROUND(total_amount * exchange_rate, 2);

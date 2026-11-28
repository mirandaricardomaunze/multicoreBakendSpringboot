-- Origem das encomendas (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, API-08): as criadas pela API pública
-- guardam o nome da chave, a referência externa e o telefone do Consumidor Final.
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS origin VARCHAR(20) NOT NULL DEFAULT 'DESKTOP';
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS origin_name VARCHAR(120);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS external_reference VARCHAR(80);
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(40);

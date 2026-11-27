-- Idempotência das encomendas da API pública (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.2, API-09): impressão
-- digital SHA-256 do corpo normalizado, para distinguir uma repetição (200) de um conflito (409).
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS request_fingerprint VARCHAR(64);

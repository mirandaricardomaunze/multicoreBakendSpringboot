-- Webhook subscriptions per company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.1.1, WH-01).
--
-- URL and events never change after creation; only active does. The whsec_ secret is stored encrypted at rest
-- (AES-256-GCM, MCE1 prefix, Base64 text) because the HMAC signature of each delivery needs it in clear.
-- consecutive_failures counts failed deliveries in a row; 20 deactivate the subscription (WH-07).
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id                   BIGSERIAL PRIMARY KEY,
    company_id           BIGINT       NOT NULL REFERENCES companies(id),
    url                  VARCHAR(500) NOT NULL,
    events               VARCHAR(200) NOT NULL,
    secret_ciphertext    VARCHAR(255) NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    consecutive_failures INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_webhook_subscriptions_company ON webhook_subscriptions (company_id);

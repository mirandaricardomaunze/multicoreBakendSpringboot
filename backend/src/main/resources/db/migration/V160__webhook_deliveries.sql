-- Webhook deliveries (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.2, WH-03).
--
-- One row per event x interested subscription, recorded after the business commit in a new transaction with the
-- JSON body already serialized (retries and resends send the same text and event_id). status is PENDING,
-- DELIVERED or FAILED; attempts, next_attempt_at and last_* are filled by the sender (WH-04/WH-05). Rows older
-- than 30 days are cleaned by WH-06.
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT       NOT NULL REFERENCES companies(id),
    subscription_id  BIGINT       NOT NULL REFERENCES webhook_subscriptions(id),
    event_id         VARCHAR(60)  NOT NULL,
    event_type       VARCHAR(40)  NOT NULL,
    body             TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMP,
    last_http_status INTEGER,
    last_duration_ms BIGINT,
    last_error       VARCHAR(500),
    delivered_at     TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_due ON webhook_deliveries (status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_subscription ON webhook_deliveries (subscription_id, created_at);

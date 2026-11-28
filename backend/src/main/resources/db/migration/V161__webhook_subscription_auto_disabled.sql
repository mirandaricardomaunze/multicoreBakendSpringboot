-- Automatic deactivation after 20 failed deliveries in a row (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.3.4, WH-07).
--
-- auto_disabled_at is set only by the automatic deactivation (never by a manual one) and drives the ADMIN alert;
-- reactivating the subscription clears it together with consecutive_failures.
ALTER TABLE webhook_subscriptions ADD COLUMN IF NOT EXISTS auto_disabled_at TIMESTAMP NULL;

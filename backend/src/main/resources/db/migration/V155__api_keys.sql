-- Public API keys per company (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.1, API-01).
--
-- The full key (mk_live_ + 32 random bytes in base62) is shown once and never stored: only its SHA-256 hash
-- (64 hex characters) and the first 8 characters after the prefix, to identify it. Revoking sets revoked_at.
CREATE TABLE IF NOT EXISTS api_keys (
    id           BIGSERIAL PRIMARY KEY,
    company_id   BIGINT       NOT NULL REFERENCES companies(id),
    name         VARCHAR(100) NOT NULL,
    key_prefix   VARCHAR(8)   NOT NULL,
    key_hash     VARCHAR(64)  NOT NULL,
    permissions  VARCHAR(100) NOT NULL,
    last_used_at TIMESTAMP,
    revoked_at   TIMESTAMP,
    revoked_by   VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(255),
    CONSTRAINT uk_api_keys_key_hash UNIQUE (key_hash)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_company ON api_keys (company_id);

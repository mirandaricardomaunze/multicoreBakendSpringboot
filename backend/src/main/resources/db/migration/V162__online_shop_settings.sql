-- Online shop settings per company (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01).
--
-- One row per company, created when the ADMIN first saves the settings; disabling keeps the row. public_code is the
-- {code} of /loja/{code}: lower-case letters, digits and hyphens, unique in the whole system (NULL allowed while the
-- shop was never enabled). Pickup stores are sales warehouses of the company; delivery zones carry a fee >= 0.
CREATE TABLE IF NOT EXISTS online_shop_settings (
    id               BIGSERIAL PRIMARY KEY,
    company_id       BIGINT      NOT NULL UNIQUE REFERENCES companies(id),
    enabled          BOOLEAN     NOT NULL DEFAULT FALSE,
    public_code      VARCHAR(50) UNIQUE,
    contact_phone    VARCHAR(30),
    auto_cancel_days INTEGER     NOT NULL DEFAULT 7,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    CONSTRAINT ck_online_shop_auto_cancel_days CHECK (auto_cancel_days >= 1)
);

CREATE TABLE IF NOT EXISTS online_shop_pickup_warehouses (
    settings_id  BIGINT  NOT NULL REFERENCES online_shop_settings(id) ON DELETE CASCADE,
    sort_order   INTEGER NOT NULL,
    warehouse_id BIGINT  NOT NULL REFERENCES warehouses(id),
    PRIMARY KEY (settings_id, sort_order)
);

CREATE TABLE IF NOT EXISTS online_shop_delivery_zones (
    settings_id BIGINT         NOT NULL REFERENCES online_shop_settings(id) ON DELETE CASCADE,
    sort_order  INTEGER        NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    fee         NUMERIC(19, 2) NOT NULL,
    PRIMARY KEY (settings_id, sort_order),
    CONSTRAINT ck_online_shop_delivery_zone_fee CHECK (fee >= 0)
);

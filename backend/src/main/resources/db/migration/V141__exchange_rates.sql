-- Tabela diária de câmbios por empresa (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.2, FX-02).
--
-- rate = quantas unidades da moeda base vale 1 unidade de currency (ex.: USD = 63,9000), > 0, 4 casas.
-- Um câmbio por empresa, moeda e data; a lista fechada de moedas e "a moeda base não tem câmbio" vivem no serviço.
CREATE TABLE IF NOT EXISTS exchange_rates (
    id            BIGSERIAL PRIMARY KEY,
    company_id    BIGINT         NOT NULL REFERENCES companies(id),
    currency      VARCHAR(3)     NOT NULL,
    rate_date     DATE           NOT NULL,
    rate          NUMERIC(19, 4) NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    CONSTRAINT uk_exchange_rates_company_currency_date UNIQUE (company_id, currency, rate_date),
    CONSTRAINT ck_exchange_rates_rate_positive CHECK (rate > 0)
);

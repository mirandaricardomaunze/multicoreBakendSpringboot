-- Moeda base por empresa (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.1, FX-01).
--
-- Código ISO 4217 em que ficam stock, custo médio, contabilidade e relatórios. As empresas existentes
-- ficam com MZN pelo DEFAULT; a regra "não muda depois do primeiro movimento" vive no serviço.
ALTER TABLE companies ADD COLUMN IF NOT EXISTS base_currency VARCHAR(3) DEFAULT 'MZN' NOT NULL;

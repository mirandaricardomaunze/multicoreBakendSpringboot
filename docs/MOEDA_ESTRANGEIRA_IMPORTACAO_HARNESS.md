# Harness — Compras em moeda estrangeira e custos de importação

> Cenários que provam [MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md](MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-22 (FX-01, FX-02, FX-03, FX-04, FX-11 verdes)

## Moeda e câmbio (GOAL-008)

| ID | Garantia | Teste |
|---|---|---|
| FX-01 | Moeda base por empresa, MZN por omissão; migração põe MZN nas existentes; não muda depois do primeiro movimento — compra, factura ou movimento de stock (§3.1) | `CompanyBaseCurrencyTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| FX-02 | Só MZN, USD, ZAR, EUR; câmbio > 0 com 4 casas; um por empresa, moeda e data; a moeda base não entra na tabela (câmbio 1) (§3.2) | `ExchangeRateRulesTest` · `MigrationPostgresDialectHarnessTest` |
| FX-03 | Câmbio proposto = o da data ou o anterior mais recente; sem nenhum, o documento exige câmbio escrito (§3.2) | `ExchangeRateRulesTest` |
| FX-04 | Corrigir um câmbio da tabela não altera documentos já gravados (§3.2) | `ExchangeRateRulesTest` |
| FX-05 | Encomenda e compra guardam moeda e câmbio; moeda base tem câmbio 1; compras antigas migram com câmbio 1 (§3.3) | `ForeignCurrencyPurchaseTest` |
| FX-06 | Total em moeda base = total × câmbio (2 casas, HALF_UP); custo de stock e custo médio = preço × câmbio (§3.3) | `ForeignCurrencyPurchaseTest` |
| FX-07 | Lançamento da compra em moeda base ao câmbio do documento (§3.3) | `ForeignCurrencyPostingTest` |
| FX-08 | Contas a pagar e extracto de fornecedor mostram moeda, dívida na moeda do documento e equivalente em moeda base (§3.3) | `ForeignCurrencyPurchaseTest` |
| FX-09 | Pagamento: sai valor × câmbio do pagamento; dívida baixa ao câmbio do documento, proporcional; liquidação sem resíduos (§3.4) | `ExchangeDifferenceTest` |
| FX-10 | Diferença cambial lançada em 6601 (perda) ou 7601 (ganho); lançamento equilibrado (§3.4) | `ExchangeDifferenceTest` |
| FX-11 | Plano de contas com 66/6601 e 76/7601, também nas empresas existentes, sem duplicar (§3.4) | `ExchangeDifferenceAccountsTest` · `MigrationPostgresDialectHarnessTest` |
| FX-12 | Endpoints de câmbios (criar/corrigir só ADMIN), compra e pagamento em moeda estrangeira; empresa activa (§3) | `ForeignCurrencyHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| FX-13 | PDFs de encomenda e compra mostram moeda e câmbio (§3.3) | `ForeignCurrencyHttpIntegrationTest` |
| FX-UI-01 | Moeda e câmbio nos formulários de encomenda/compra; contas a pagar com moeda (§4.5) | `ForeignCurrencyUiHarnessTest` |
| FX-UI-02 | Pagamento com câmbio e diferença cambial antes de confirmar (§4.5) | `ForeignCurrencyUiHarnessTest` |
| FX-UI-03 | Configurações: tabela de câmbios e moeda base só leitura com movimentos (§4.5) | `ForeignCurrencyUiHarnessTest` |

## Custos de importação (GOAL-009)

| ID | Garantia | Teste |
|---|---|---|
| LC-01 | Documento CI numerado `CI-aaaa/n` por empresa, ligado a compras `COMPLETED` da empresa; várias CI por compra (§4.1) | `LandedCostDocumentTest` |
| LC-02 | Linhas com tipo, fornecedor, moeda/câmbio, valor e IVA; cada linha gera dívida ao seu fornecedor, paga pela acção normal (§4.1) | `LandedCostDocumentTest` |
| LC-03 | Custo = linhas não-IVA sem IVA em moeda base; IVA aduaneiro e IVA dos serviços fora do custo (§4.2) | `LandedCostAllocationTest` |
| LC-04 | Repartição por VALOR, PESO ou QUANTIDADE; PESO sem peso numa linha é erro (§4.3) | `LandedCostAllocationTest` |
| LC-05 | Arredondamento a 2 casas com resíduo na maior quota; soma igual ao total (§4.3) | `LandedCostAllocationTest` |
| LC-06 | Parte para o stock = C × min(1, S/Q) sobe o custo médio; resto a CMV; vendas passadas inalteradas (§4.4) | `LandedCostStockEffectTest` |
| LC-07 | Lançamento equilibrado: 3201 + 6101 + 2432 a débito, 2201 a crédito (§4.4) | `LandedCostPostingTest` |
| LC-08 | Pré-visualização por artigo sem gravar; confirmação grava tudo numa transacção; empresa activa; perfis de compras (§4.3, §4.4) | `LandedCostHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LC-UI-01 | Compras › Custos de Importação: compras, linhas, método, pré-visualização e confirmação (§4.5) | `LandedCostUiHarnessTest` |

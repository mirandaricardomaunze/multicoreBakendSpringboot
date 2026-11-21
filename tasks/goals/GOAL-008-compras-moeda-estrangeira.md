---
id: GOAL-008
title: "Comprar e pagar fornecedores em USD, ZAR e EUR com câmbio e diferença cambial"
status: IN_PROGRESS
priority: HIGH
spec: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md
harness: docs/MOEDA_ESTRANGEIRA_IMPORTACAO_HARNESS.md
verify: ["CompanyBaseCurrencyTest", "ExchangeRateRulesTest", "ForeignCurrencyPurchaseTest", "ForeignCurrencyPostingTest", "ExchangeDifferenceTest", "ExchangeDifferenceAccountsTest", "ForeignCurrencyHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "ForeignCurrencyUiHarnessTest"]
blockedBy: []
---

# Objectivo

O comprador regista encomendas e compras na moeda do fornecedor (USD, ZAR, EUR), com o câmbio do
dia gravado no documento. O stock, o custo médio e a contabilidade ficam correctos em meticais, e o
pagamento feito mais tarde, a outro câmbio, lança sozinho a diferença cambial.
Regras em `docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md` §2–§3 e §4.5.

## Critérios de aceitação

- [x] FX-01 — moeda base por empresa, MZN, imutável após movimentos — `CompanyBaseCurrencyTest`
- [x] FX-02 — moedas aceites e regras do câmbio — `ExchangeRateRulesTest`
- [x] FX-03 — câmbio proposto pela data ou anterior; sem câmbio exige escrita — `ExchangeRateRulesTest`
- [x] FX-04 — corrigir câmbio não altera documentos gravados — `ExchangeRateRulesTest`
- [ ] FX-11 — contas 6601 e 7601 semeadas, também em empresas existentes — `ExchangeDifferenceAccountsTest`
- [ ] FX-05 — encomenda e compra com moeda e câmbio; antigas com câmbio 1 — `ForeignCurrencyPurchaseTest`
- [ ] FX-06 — total em moeda base e custo médio convertidos — `ForeignCurrencyPurchaseTest`
- [ ] FX-08 — contas a pagar e extracto com moeda e dívida na moeda — `ForeignCurrencyPurchaseTest`
- [ ] FX-07 — lançamento da compra em moeda base — `ForeignCurrencyPostingTest`
- [ ] FX-09 — pagamento: câmbio do pagamento, baixa ao câmbio do documento, sem resíduos — `ExchangeDifferenceTest`
- [ ] FX-10 — diferença cambial em 6601/7601, lançamento equilibrado — `ExchangeDifferenceTest`
- [ ] FX-12 — endpoints de câmbios, compra e pagamento; ADMIN para câmbios — `ForeignCurrencyHttpIntegrationTest`
- [ ] FX-13 — PDFs com moeda e câmbio — `ForeignCurrencyHttpIntegrationTest`
- [ ] FX-UI-03 — tabela de câmbios e moeda base em Configurações — `ForeignCurrencyUiHarnessTest`
- [ ] FX-UI-01 — moeda e câmbio em encomenda/compra; contas a pagar com moeda — `ForeignCurrencyUiHarnessTest`
- [ ] FX-UI-02 — pagamento com câmbio e diferença cambial visível — `ForeignCurrencyUiHarnessTest`

## Fora de âmbito

- Custos de importação (GOAL-009).
- Vendas em moeda estrangeira, tesouraria em moeda estrangeira, reavaliação cambial, câmbio
  automático (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-22 · FX-01 · `Company.baseCurrency` (MZN por omissão, V140 põe MZN nas existentes), `CurrencyCode` com a lista fechada MZN/USD/ZAR/EUR e `CompanyBaseCurrencyService` que só deixa mudar a moeda base sem compras, facturas nem movimentos de stock (sondas `CompanyMovementProbe` em cada módulo dono); `CompanyBaseCurrencyTest` 7/7.
- 2026-11-22 · FX-02 · novo módulo `currency` com `ExchangeRate` (tabela `exchange_rates`, V141, chave empresa+moeda+data e `CHECK (rate > 0)`) e `ExchangeRateService.create/list`: só MZN/USD/ZAR/EUR, a moeda base fica fora da tabela (câmbio sempre 1), câmbio > 0 com no máximo 4 casas (gravado com 4), um por empresa, moeda e data, só na empresa activa; `ExchangeRateRulesTest` 9/9.
- 2026-11-22 · FX-03 · `ExchangeRateService.propose` devolve `ProposedExchangeRateDTO` com o câmbio da data ou da data anterior mais recente (moeda base sempre 1; sem câmbio, `manualRateRequired`) e `resolveDocumentRate` usa o câmbio escrito (validado como na tabela) ou o proposto, e recusa com "Escreva o câmbio do documento" quando não há nenhum; `ExchangeRateRulesTest` 15/15.
- 2026-11-22 · FX-04 · `ExchangeRateService.correct` (`CorrectExchangeRateRequest`) corrige só o valor de um câmbio da empresa activa, com as mesmas regras da tabela; o documento guarda o valor (fotografia) devolvido por `resolveDocumentRate`, nunca uma FK para `exchange_rates` (verificado nas migrações), por isso a correcção só afecta documentos novos; `ExchangeRateRulesTest` 20/20.

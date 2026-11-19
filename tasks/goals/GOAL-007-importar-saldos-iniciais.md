---
id: GOAL-007
title: "Importar stock inicial e dívidas em aberto de clientes e fornecedores"
status: TODO
priority: HIGH
spec: docs/IMPORTACAO_DADOS_SPEC.md
harness: docs/IMPORTACAO_DADOS_HARNESS.md
verify: ["StockOpeningImportServiceTest", "OpeningReceivableTest", "OpeningReceivableFiscalExclusionTest", "OpeningReceivableStatementTest", "OpeningReceivableSettlementTest", "OpeningPayableTest", "OpeningBalancesImportHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "ModuleDecouplingHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DataImportPanelHarnessTest"]
blockedBy: []
---

# Objectivo

Depois dos cadastros (GOAL-006), a empresa arranca com o stock que tem nas prateleiras e com as
facturas que os clientes lhe devem e que ela deve aos fornecedores, cada uma com o seu vencimento,
para que extractos, antiguidade de saldos, cobranças e pagamentos funcionem desde o primeiro dia.
Regras em `docs/IMPORTACAO_DADOS_SPEC.md` §5. Reutiliza o leitor CSV, o parser e o fluxo
pré-visualizar/confirmar do GOAL-006.

Pré-requisito: GOAL-006 em `DONE` (usa `CsvImportReader`, `ImportValueParser` e o módulo
`dataimport`). Se não estiver, a iteração termina com `NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [ ] IMP-ST-01 — stock: referências, quantidade > 0, custo ≥ 0, validade quando exigida — `StockOpeningImportServiceTest`
- [ ] IMP-ST-02 — stock: artigo com movimentos no armazém é erro; reimportar não duplica — `StockOpeningImportServiceTest`
- [ ] IMP-ST-03 — stock: `ENTRY` com lote e saldo pelo ledger, sem lançamento contabilístico — `StockOpeningImportServiceTest`
- [ ] IMP-SI-01 — documento SI: numeração por empresa, nº original, chave cliente+nº original — `OpeningReceivableTest`
- [ ] IMP-SI-02 — SI: valor > 0, vencimento ≥ data, data não futura, cliente existente — `OpeningReceivableTest`
- [ ] IMP-SI-03 — SI fora do SAF-T, IVA e relatórios de vendas — `OpeningReceivableFiscalExclusionTest`
- [ ] IMP-SI-04 — SI no extracto, antiguidade, exposição de crédito e mora — `OpeningReceivableStatementTest`
- [ ] IMP-SI-05 — SI liquida-se com recibos normais, total ou parcial — `OpeningReceivableSettlementTest`
- [ ] IMP-SIF-01 — SIF: chave, valores, datas e numeração — `OpeningPayableTest`
- [ ] IMP-SIF-02 — SIF em contas a pagar e pagamento a fornecedor, total ou parcial — `OpeningPayableTest`
- [ ] IMP-12 — endpoints de stock inicial, SI e SIF: tudo ou nada, empresa activa, ADMIN — `OpeningBalancesImportHttpIntegrationTest`
- [ ] IMP-UI-03 — ecrã "Importar dados" com stock inicial, dívidas de clientes e a fornecedores — `DataImportPanelHarnessTest`

## Fora de âmbito

- Lançamentos contabilísticos de abertura, documentos históricos já pagos, movimentos de stock
  passados (SPEC §6).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

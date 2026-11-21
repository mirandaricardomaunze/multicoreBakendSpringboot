---
id: GOAL-007
title: "Importar stock inicial e dívidas em aberto de clientes e fornecedores"
status: IN_PROGRESS
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

- [x] IMP-ST-01 — stock: referências, quantidade > 0, custo ≥ 0, validade quando exigida — `StockOpeningImportServiceTest`
- [x] IMP-ST-02 — stock: artigo com movimentos no armazém é erro; reimportar não duplica — `StockOpeningImportServiceTest`
- [x] IMP-ST-03 — stock: `ENTRY` com lote e saldo pelo ledger, sem lançamento contabilístico — `StockOpeningImportServiceTest`
- [x] IMP-SI-01 — documento SI: numeração por empresa, nº original, chave cliente+nº original — `OpeningReceivableTest`
- [x] IMP-SI-02 — SI: valor > 0, vencimento ≥ data, data não futura, cliente existente — `OpeningReceivableTest`
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

- **2026-11-20 — Resposta (responsável via Claude):** falso alarme. O pacote `units/dto` está no git desde `380425a`; `mvn clean test-compile` na worktree passa (exit 0). A falha veio de um build intermédio. Desbloqueado: continuar.

- **2026-11-19 — Pré-requisito por cumprir.** O GOAL-007 exige o GOAL-006 em `DONE` (usa
  `CsvImportReader`, `ImportValueParser` e o módulo `dataimport`), mas o GOAL-006 está `BLOCKED`, só
  com IMP-01 marcado. O backend não compila porque o pacote `modules/units/dto` do commit `3218952`
  nunca foi commitado. Ver a pergunta em aberto no GOAL-006.
  Opções:
  1. **(recomendado)** Desbloquear e terminar o GOAL-006 primeiro: commitar os DTOs de `units/dto`
     e deixar o loop acabar IMP-02..IMP-UI-01. Depois voltar este goal a `TODO` e esvaziar `blockedBy`.
  2. Começar já as partes do GOAL-007 que não dependem do `dataimport`. Não recomendado: o build está
     partido, por isso nenhum teste se consegue provar verde, e isso contraria o pré-requisito escrito.
  Para desbloquear: GOAL-006 em `DONE`, depois `status: TODO` e `blockedBy: []` aqui.

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-19 · — · não começado: pré-requisito GOAL-006 está `BLOCKED` (build partido, `units/dto` em falta) → BLOCKED
- 2026-11-21 · IMP-ST-01 · `StockOpeningImportService` valida o stock inicial (artigo e armazém da empresa, armazém por código ou nome, quantidade > 0, custo ≥ 0, validade de perecível, lote vencido, SKU+armazém+lote repetido no ficheiro) com `StockOpeningImportServiceTest` verde 20/20
- 2026-11-21 · IMP-ST-02 · artigo com movimentos no armazém é `ERRO` na coluna `armazem` ("já tem stock neste armazém; use um ajuste"), lido pela porta `StockMovementLookup.listProductIdsWithMovements` (uma consulta por armazém); reimportar o ficheiro já gravado dá `ERRO` em todas as linhas; `StockOpeningImportServiceTest` verde 23/23, `InventoryDocumentLookupsTest` 6/6
- 2026-11-21 · gate · corrigida a falha intermitente do `DataImportPanelHarnessTest` (ArrayIndexOutOfBounds no `DefaultRowSorter`): os testes IMP-UI-01 passam a correr o painel na EDT (`SwingUtilities.invokeAndWait`), sem retirar asserções; verde 8/8 em três execuções seguidas, `DesktopThinContextTest` verde
- 2026-11-21 · IMP-ST-03 · `StockOpeningWriter` grava cada linha pelo `StockMovementService` como `ENTRY` com lote e validade (descrição "Stock inicial (importação)"), depois do `AverageCostUpdater.beforeEntry` ao custo do ficheiro; ficheiro com erro não grava nada; sem acerto nem lançamento contabilístico; SPEC §5.1 clarificada; `StockOpeningImportServiceTest` verde 26/26, harness de arquitectura verdes
- 2026-11-21 · IMP-SI-01 · documento "Saldo Inicial" em `invoices` com `document_kind = OPENING_BALANCE`, `original_number`, `document_date` e `description` (V138, chave única empresa+cliente+nº original); série `SI` em `DocumentSeries`; `OpeningReceivableCommandService` (comercial) cria o SI emitido e por receber, sem linhas nem IVA nem evento de venda; `OpeningReceivableImportService` classifica pelo NUIT do cliente e pela chave (`JÁ EXISTE` / repetida → `ERRO`) e `OpeningReceivableWriter` grava tudo ou nada; SPEC §5.2 clarificada; `OpeningReceivableTest` verde 10/10
- 2026-11-21 · IMP-SI-02 · `OpeningReceivableImportService` dá `ERRO` na coluna certa a data futura (`data`), vencimento anterior à data (`vencimento`) e valor ≤ 0 (`valor_em_divida`), pela ordem das colunas; cliente inexistente já era `ERRO` em `nuit_cliente`; `OpeningReceivableCommandService` recusa também valor ≤ 0 e data futura (vencimento pelo `Invoice.assignDueDate`); `OpeningReceivableTest` verde 18/18, `StockOpeningImportServiceTest` e `ModuleDecouplingHarnessTest` verdes

---
id: GOAL-007
title: "Importar stock inicial e dívidas em aberto de clientes e fornecedores"
status: IN_PROGRESS
priority: HIGH
spec: docs/IMPORTACAO_DADOS_SPEC.md
harness: docs/IMPORTACAO_DADOS_HARNESS.md
verify: ["StockOpeningImportServiceTest", "OpeningReceivableTest", "OpeningReceivableFiscalExclusionTest", "OpeningReceivableStatementTest", "OpeningReceivableSettlementTest", "OpeningPayableTest", "VatSettlementHarnessTest", "OpeningBalancesImportHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "ModuleDecouplingHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DataImportPanelHarnessTest"]
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
- [x] IMP-SI-03 — SI fora do SAF-T, IVA e relatórios de vendas — `OpeningReceivableFiscalExclusionTest`
- [x] IMP-SI-04 — SI no extracto, antiguidade, exposição de crédito e mora — `OpeningReceivableStatementTest`
- [x] IMP-SI-05 — SI liquida-se com recibos normais, total ou parcial — `OpeningReceivableSettlementTest`
- [x] IMP-SIF-01 — SIF: chave, valores, datas e numeração — `OpeningPayableTest`
- [x] IMP-SIF-02 — SIF em contas a pagar e pagamento a fornecedor, total ou parcial — `OpeningPayableTest`
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
- 2026-11-21 · IMP-SI-03 · `Invoice.isSale()` / `countsAsSale()` (fonte única: natureza `SALE` e, no segundo, estado de venda realizada); o SI fica fora do ficheiro SAF-T (`FiscalSalesExportService`), do apuramento e declaração de IVA (`FiscalSummaryService`), do painel e relatório diário da loja (`ReportSalesAnalyzer`) e das metas/ranking comerciais, e continua "por cobrar"; `OpeningReceivableFiscalExclusionTest` verde 5/5, `FiscalSalesExportServiceTest`, `VatSettlementHarnessTest`, `ReportServiceTest`, `CommercialPerformanceHarnessTest`, `AiManagementAssistantServiceTest` e `ModuleDecouplingHarnessTest` verdes
- 2026-11-21 · IMP-SI-04 · o SI entra no extracto como débito na data da factura antiga, tipo `SI`, descrição "Saldo Inicial — <nº original>" e o nº original como referência (`CustomerStatementCollector`); saldo transportado quando a data é anterior ao período, valor vencido do extracto, antiguidade pelo vencimento (`ReceivablesAgingCalculator`), exposição ao limite de crédito e bloqueio por mora (`CreditControlService`) já o tratavam como factura e ficam provados; o painel do extracto mostra o `SI` como débito; SPEC §5.2 clarificada; `OpeningReceivableStatementTest` verde 7/7, `AccountStatementHarnessTest`, `CustomerStatementServiceDecompositionHarnessTest`, `InvoiceAgingTest`, `OpeningReceivableTest`, `OpeningReceivableFiscalExclusionTest`, `ModuleDecouplingHarnessTest` e `DesktopThinContextTest` verdes
- 2026-11-21 · IMP-SI-05 · o SI liquida-se com recibos `RC` normais (`ComercialService.createReceipt`) e pela liquidação da tesouraria (`InvoiceSettlementService`, sem valor = tudo o que falta): parcial → `PARTIALLY_PAID`, resto → `PAID`, valor acima do saldo recusado, anular o recibo devolve a dívida, recibo a crédito no extracto com referência `SI-aaaa/n`; o movimento de tesouraria passa a dizer "Recebimento Saldo Inicial SI-… (<nº original>)" via `Invoice.collectionLabel()` (vendas mantêm "Fatura …"); SPEC §5.2 clarificada; `OpeningReceivableSettlementTest` verde 7/7, `ComercialServiceTest` 60/60, `OpeningReceivable*Test` e `ModuleDecouplingHarnessTest` verdes
- 2026-11-21 · IMP-SIF-01 · documento "Saldo Inicial Fornecedor" em `purchases` com `document_kind = OPENING_BALANCE`, `original_number`, `due_date` e `description`, sem armazém (V139: `warehouse_id` passa a opcional, chave única empresa+fornecedor+nº original); série `SIF` em `DocumentSeries`; `OpeningPayableCommandService` (compras) cria o SIF por pagar (data da factura antiga como `purchase_date`), sem linhas, IVA, stock nem evento de compra, e recusa valor ≤ 0, data futura, vencimento anterior e chave já importada; `OpeningPayableImportService` + `OpeningPayableWriter` pelo NUIT do fornecedor; colunas e regras comuns ao SI extraídas para `OpeningDebtColumns`; backup tolera compra sem armazém; SPEC §5.3 clarificada; `OpeningPayableTest` verde 14/14, `OpeningReceivableTest` 18/18. Atenção para o IMP-SIF-02: o `FiscalSummaryService` ainda lista todas as `purchases` nas compras do IVA (o SIF entra com IVA 0 mas soma à base) — excluir o `OPENING_BALANCE` ("não gera documento fiscal", §5.3)
- 2026-11-21 · IMP-SIF-02 · o SIF aparece nas contas a pagar (`SupplierPaymentService.findPayablesByCompany`, sai quando pago) e paga-se com o pagamento normal a fornecedor: parcial deixa o resto, segundo pagamento liquida, valor acima do saldo recusado sem mexer na tesouraria; o movimento de tesouraria diz "Saldo Inicial Fornecedor SIF-… (<nº original>)" via `Purchase.paymentLabel()` (compras mantêm "Compra …") e o evento leva o `SIF-…`; `Purchase.isPurchase()` tira o SIF do apuramento de IVA (`FiscalSummaryService`); SPEC §5.3 clarificada; `OpeningPayableTest` verde 20/20, `VatSettlementHarnessTest`, `OpeningReceivableFiscalExclusionTest`, `PurchaseServiceTest`, `PurchaseOrderPayableHarnessTest`, `ModuleDecouplingHarnessTest` e `CleanArchitectureHarnessTest` verdes

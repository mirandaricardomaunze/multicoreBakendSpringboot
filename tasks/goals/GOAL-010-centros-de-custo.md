---
id: GOAL-010
title: "Resultado por loja, armazém e departamento com centros de custo"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md
harness: docs/CENTROS_CUSTO_ORCAMENTO_HARNESS.md
verify: ["CostCenterRulesTest", "CostCenterWarehouseSyncTest", "JournalLineCostCenterTest", "AutomaticPostingCostCenterTest", "CostCenterResultReportTest", "CostCenterHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "CostCenterUiHarnessTest"]
blockedBy: []
---

# Objectivo

Cada custo e proveito da contabilidade fica marcado com a loja, o armazém ou o departamento que o
gerou, quase sempre sem ninguém escolher (vem do armazém do documento), e o gestor vê o resultado de
cada centro. Regras em `docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md` §2–§3.

## Critérios de aceitação

- [x] CC-01 — cadastro de centros, código único, inactivo e apagar com lançamentos — `CostCenterRulesTest`
- [x] CC-03 — centro `ADM` por omissão por empresa, alterável — `CostCenterRulesTest`
- [x] CC-02 — centro automático por armazém, existentes migrados, acompanha o armazém — `CostCenterWarehouseSyncTest`
- [x] CC-04 — obrigatório em 6/7, proibido em 1–5, mesma empresa — `JournalLineCostCenterTest`
- [x] CC-06 — manual e despesa com centro ou por omissão; linhas antigas migradas — `JournalLineCostCenterTest`
- [x] CC-05 — automáticos herdam do armazém ou usam o por omissão — `AutomaticPostingCostCenterTest`
- [ ] CC-07 — resultados por centro iguais à demonstração de resultados — `CostCenterResultReportTest`
- [ ] CC-08 — razão e balancete filtráveis por centro — `CostCenterResultReportTest`
- [ ] CC-09 — endpoints, perfis e empresa activa — `CostCenterHttpIntegrationTest`
- [ ] CC-UI-01 — gestão de centros e centro por omissão — `CostCenterUiHarnessTest`
- [ ] CC-UI-02 — lançamento manual e despesa pedem o centro — `CostCenterUiHarnessTest`
- [ ] CC-UI-03 — ecrã de resultados por centro — `CostCenterUiHarnessTest`

## Fora de âmbito

- Orçamento (GOAL-011); repartição de custos comuns (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-22 · CC-01 · cadastro de centros de custo por empresa (código único, tipo WAREHOUSE/MANUAL, renomear/desactivar/apagar só manuais, não apaga com lançamentos, inactivo ou de outra empresa recusa lançamentos) em `CostCenterService`, migração V148 com `journal_lines.cost_center_id`; CostCenterRulesTest verde 14/14.
- 2026-11-22 · CC-03 · centro por omissão por empresa (`cost_centers.default_center`, migração V149 cria `ADM — Administração` em cada empresa e marca-o); `CostCenterService.requireDefault` devolve-o ou cria/promove o `ADM`, `setDefault` muda para outro centro activo da empresa (ADMIN/MANAGER), o por omissão não se desactiva nem apaga; regra completada na SPEC §3.1; CostCenterRulesTest verde 23/23.
- 2026-11-22 · CC-02 · centro automático por armazém: `WarehouseCommandService` (única escrita de armazéns) anuncia `WarehouseSavedEvent` e `CostCenterWarehouseSync` cria o centro WAREHOUSE (código = número do armazém, senão `ARM-<id>`) ou faz acompanhar nome e estado; arranque e migração V150 dão centro aos armazéns existentes; desactivar o armazém do centro por omissão devolve-o ao `ADM`; regra completada na SPEC §3.1; CostCenterWarehouseSyncTest verde 7/7.
- 2026-11-22 · CC-04 · regra no domínio (`JournalLine.validateCostCenter`, `JournalEntry.validateCostCenters`): centro obrigatório nas classes 6/7, recusado nas classes 1–5, recusado se inactivo ou de outra empresa; `JournalService.save` (porta única) completa partidas 6/7 sem centro com o centro por omissão e aplica a regra, para os lançamentos automáticos não partirem (herança do armazém fica para CC-05, `costCenterId` no pedido e migração das linhas antigas para CC-06); SPEC §3.2 clarificada; JournalLineCostCenterTest verde 9/9, testes de lançamentos existentes verdes.
- 2026-11-22 · CC-06 · `CreateJournalEntryRequest.Line.costCenterId` e `CreateExpenseClaimRequest.costCenterId` opcionais; `JournalService.costCenterOrNull` (único ponto que valida o centro pedido) serve o lançamento manual e o reembolso da despesa (`ExpenseClaim.costCenterId` → `FinanceService.registerAutoExpensePayout` → `TreasuryPayoutEvent.costCenterId` → partida 6201); vazio em 6/7 cai no por omissão, centro em 1–5 recusado; migração V151 (coluna `expense_claims.cost_center_id` e linhas 6/7 antigas no centro por omissão); SPEC §3.2 clarificada; JournalLineCostCenterTest verde 18/18, HRServiceTest e TreasuryPayoutPostingServiceTest verdes.
- 2026-11-22 · CC-05 · eventos de venda/POS, notas de crédito/débito, valor de stock (quebra, ajuste, desmancha, produção), produção concluída e pagamento a fornecedor levam `warehouseId`; custos de importação levam a parte vendida por armazém (`soldByWarehouse`); `CostCenterService.postingCenterOf` dá o centro activo do armazém ou o por omissão e `JournalService.assignWarehouseCostCenter` aplica-o às partidas 6/7 antes do `save`; estorno de venda/recebimento mantém o centro da original; SPEC §3.2 concretizada; AutomaticPostingCostCenterTest verde 14/14, AutomaticPostingServiceTest, ExchangeDifferenceTest, LandedCost*Test e harnesses de decomposição verdes.

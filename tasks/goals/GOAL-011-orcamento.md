---
id: GOAL-011
title: "Orçamento anual por centro e alertas de excesso"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md
harness: docs/CENTROS_CUSTO_ORCAMENTO_HARNESS.md
verify: ["BudgetRulesTest", "BudgetVarianceTest", "BudgetAlertTest", "BudgetHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "BudgetUiHarnessTest"]
blockedBy: []
---

# Objectivo

O gestor planeia o ano mês a mês por conta e centro (ou parte do real do ano anterior), vê o real
contra o orçado com o desvio, e é avisado quando um centro chega a 90% e 100% dos custos do mês.
Regras em `docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md` §4.

Pré-requisito: GOAL-010 em `DONE` (centros nos lançamentos). Se não estiver, a iteração termina com
`NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [x] OR-01 — orçamento anual com linhas válidas e únicas — `BudgetRulesTest`
- [x] OR-02 — copiar do ano anterior com ajuste, só células vazias — `BudgetRulesTest`
- [x] OR-03 — real por conta × centro × mês — `BudgetVarianceTest`
- [x] OR-04 — desvio, desvio % e sentido favorável/desfavorável — `BudgetVarianceTest`
- [x] OR-05 — alertas 90%/100% sem repetir — `BudgetAlertTest`
- [x] OR-06 — aviso ao gravar compra/despesa, sem bloquear — `BudgetAlertTest`
- [x] OR-07 — endpoints, perfis, empresa activa e auditoria — `BudgetHttpIntegrationTest`
- [x] OR-UI-01 — grelha de orçamento anual — `BudgetUiHarnessTest`
- [ ] OR-UI-02 — real vs orçado e alertas no sino — `BudgetUiHarnessTest`

## Fora de âmbito

- Versões de orçamento, orçamento de tesouraria, aprovação/bloqueio (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-24 · OR-01 · orçamento anual por empresa e ano (`Budget`/`BudgetLine`, V152) com linhas só de contas movimentáveis 6/7, centro activo da empresa, mês 1–12, valor ≥ 0 e únicas por conta × centro × mês; regravar actualiza as mesmas linhas; `BudgetRulesTest` verde 10/10.
- 2026-11-24 · OR-02 · `BudgetService.copyFromPreviousYear` (`CopyBudgetRequest`) grava nas células vazias o real do ano anterior por conta × centro × mês (`BudgetActualsCalculator`, sinal por classe) com ajuste % arredondado a 2 casas; não sobrescreve células preenchidas (nem a zero), ignora real ≤ 0 e centros inactivos; `BudgetRulesTest` verde 16/16.
- 2026-11-24 · OR-03 · `BudgetVarianceService.report` (`BudgetVarianceQuery` → `BudgetVarianceDTO`) dá por conta × centro o orçado e o real do mês ou acumulado de janeiro até ao mês (sinal por classe, só partidas com centro, filtro opcional por centro, ADMIN/MANAGER); `BudgetVarianceTest` verde 8/8.
- 2026-11-24 · OR-04 · cada linha do real vs orçado traz `variance` (real − orçado), `variancePercent` (÷ orçado × 100, 2 casas meio para cima, vazio com orçado zero) e `direction` (`BudgetVarianceDirection`: custo acima/proveito abaixo `UNFAVORABLE`, inverso `FAVORABLE`, desvio zero `ON_BUDGET`); `BudgetVarianceTest` verde 12/12.
- 2026-11-24 · OR-05 · `JournalService.save` publica `JournalEntryPostedEvent` com os centros das partidas de classe 6 e o `BudgetAlertService` grava (`BudgetAlert`, V153) um alerta por limiar 90%/100%, centro e mês quando o real dos custos atinge o orçado dos custos, sem nunca repetir; `listAlerts(ano)` só ADMIN/MANAGER da empresa activa; `BudgetAlertTest` verde 10/10.
- 2026-11-24 · OR-06 · cada alerta novo do `BudgetAlertService` no lançamento de uma operação (compra, despesa do colaborador, …) vira aviso (`OperationWarnings`, só depois do commit) devolvido pelo `OperationWarningsResponseAdvice` no cabeçalho `X-Operation-Warning` das respostas de sucesso, sem bloquear nem mudar o corpo; erros descartam o aviso; `BudgetAlertTest` verde 15/15.
- 2026-11-24 · OR-07 · `BudgetController` em `/api/accounting/budgets` (`GET ?year=`, `PUT`, `POST /copy-previous-year`, `GET /variance`, `GET /alerts`) na empresa activa, só ADMIN/MANAGER (política HTTP e Service, também na consulta); gravar e copiar auditam `BUDGET_CHANGE` com ano, totais de custos/proveitos e número de células; regras violadas dão 400 PT-MZ; `BudgetHttpIntegrationTest` verde 4/4.
- 2026-11-24 · OR-UI-01 · Contabilidade › "Orçamento" (aba a seguir ao Razão): `AccountingBudgetTab` com ano e "Carregar", grelha `BudgetGridModel` conta × centro × Jan…Dez + Total (vazio não segue, zero é célula, negativo/texto recusado na célula), "Adicionar linha" (`BudgetLineDialog`: só contas movimentáveis activas 6/7 e centro activo, par repetido recusado), "Remover linha", "Copiar do ano anterior" com ajuste % e confirmação, "Gravar" (`PUT` do ano inteiro); alterar só ADMIN/MANAGER; `BudgetUiHarnessTest` verde 8/8.
- 2026-11-24 · OR-UI-01 (gate) · DG-03: o combo de contas do `BudgetLineDialog` passa a `UIHelper.createComboBox` em vez de `JComboBox` cru; diff-guard verde, `BudgetUiHarnessTest` e `DesktopThinContextTest` verdes.

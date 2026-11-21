---
id: GOAL-011
title: "Orçamento anual por centro e alertas de excesso"
status: TODO
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

- [ ] OR-01 — orçamento anual com linhas válidas e únicas — `BudgetRulesTest`
- [ ] OR-02 — copiar do ano anterior com ajuste, só células vazias — `BudgetRulesTest`
- [ ] OR-03 — real por conta × centro × mês — `BudgetVarianceTest`
- [ ] OR-04 — desvio, desvio % e sentido favorável/desfavorável — `BudgetVarianceTest`
- [ ] OR-05 — alertas 90%/100% sem repetir — `BudgetAlertTest`
- [ ] OR-06 — aviso ao gravar compra/despesa, sem bloquear — `BudgetAlertTest`
- [ ] OR-07 — endpoints, perfis, empresa activa e auditoria — `BudgetHttpIntegrationTest`
- [ ] OR-UI-01 — grelha de orçamento anual — `BudgetUiHarnessTest`
- [ ] OR-UI-02 — real vs orçado e alertas no sino — `BudgetUiHarnessTest`

## Fora de âmbito

- Versões de orçamento, orçamento de tesouraria, aprovação/bloqueio (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

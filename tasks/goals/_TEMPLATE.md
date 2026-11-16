---
id: GOAL-000
title: "Resultado observável em PT-MZ"
status: TODO
priority: HIGH
spec: docs/TEMA_SPEC.md
harness: docs/TEMA_HARNESS.md
verify: ["TemaRulesTest", "TemaHttpIntegrationTest"]
blockedBy: []
# graphNode: NODE-ID   (opcional, tem de existir em tasks/graph.yaml)
---

# Objectivo

Uma ou duas frases: que problema do utilizador fica resolvido e como se nota.

## Critérios de aceitação

Um critério = uma iteração pequena e verificável. O ID é o mesmo da tabela do HARNESS.

- [ ] TM-01 — regra de domínio pura (sem Spring) — `TemaRulesTest`
- [ ] TM-02 — endpoint/serviço respeita a regra e o tenant — `TemaHttpIntegrationTest`
- [ ] TM-03 — rota classificada na `ApiRolePolicy` — `ApiRolePolicyEndpointCoverageHarnessTest`
- [ ] TM-UI-01 — ecrã desktop segue o padrão de painel — `TemaUiHarnessTest`

## Fora de âmbito

- O que esta iteração de trabalho NÃO faz.

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

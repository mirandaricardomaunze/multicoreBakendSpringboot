---
id: GOAL-014
title: "Webhooks avisam sistemas externos de vendas, encomendas e stock"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["WebhookSubscriptionTest", "ApiKeyRulesTest", "MigrationPostgresDialectHarnessTest", "WebhookEventTest", "WebhookDeliveryTest", "WebhookHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "IntegrationsUiHarnessTest"]
blockedBy: []
---

# Objectivo

O ADMIN regista um URL e escolhe eventos; quando há uma factura, nota de crédito, mudança de estado
de encomenda ou um artigo esgota/volta, o sistema envia um aviso assinado, tenta de novo se falhar e
mostra o registo de entregas. Regras em `docs/API_PUBLICA_WEBHOOKS_SPEC.md` §4.

Pré-requisito: GOAL-013 em `DONE` (ecrã de Integrações e base da API pública). Se não estiver, a
iteração termina com `NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [x] WH-01 — subscrição: ADMIN, HTTPS, sem endereços privados, segredo uma vez — `WebhookSubscriptionTest`
- [ ] WH-02 — os quatro eventos com o corpo definido — `WebhookEventTest`
- [ ] WH-03 — envio depois do commit, sem anular a operação — `WebhookEventTest`
- [ ] WH-04 — assinatura HMAC e cabeçalhos — `WebhookDeliveryTest`
- [ ] WH-05 — tentativas e `FAILED` — `WebhookDeliveryTest`
- [ ] WH-06 — registo de entregas e reenvio — `WebhookDeliveryTest`
- [ ] WH-07 — desactivação após 20 falhas com aviso — `WebhookDeliveryTest`
- [ ] WH-08 — endpoints, ADMIN, empresa activa — `WebhookHttpIntegrationTest`
- [ ] WH-UI-01 — ecrã de webhooks — `IntegrationsUiHarnessTest`

## Fora de âmbito

- Eventos de RH, contabilidade ou compras (SPEC §5).

## Perguntas em aberto

**P1 — Pré-requisito GOAL-013 por fechar.** Este goal só começa com o GOAL-013 em `DONE`, mas o GOAL-013
está `BLOCKED` no API-08 à espera das respostas P1 (via da encomenda criada pela API: aprovação vs reserva) e
P2 (nome do cliente novo por NUIT), escritas em `tasks/goals/GOAL-013-*.md`.
Opções:
- **A (recomendada)** — o dono responde a P1/P2 do GOAL-013; o loop fecha o GOAL-013 e depois volta aqui.
- B — autorizar explicitamente que o GOAL-014 avance já (os webhooks WH-01..WH-07 não dependem da criação de
  encomendas pela API), alterando o pré-requisito deste goal.

**Resposta do responsável (2026-11-27):** opção **A** — P1/P2 do GOAL-013 respondidas; o loop fecha o
GOAL-013 primeiro e depois segue para este goal.

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · — · não iniciado: pré-requisito GOAL-013 está BLOCKED (API-08); goal passa a BLOCKED com a pergunta P1.
- 2026-11-27 · — · gate vermelho corrigido (sem critério novo): @Valid em ApiKeyController.create (BIV-01) e coluna customer_orders.origin com default 'DESKTOP' na entidade para o H2 local com dados (MulticoreServicesTest verde).
- 2026-11-27 · WH-01 · regra da subscrição escrita na SPEC §4.1.1 (ADMIN, URL/eventos, https em prod, anti-SSRF com DNS, segredo whsec_ cifrado, auditoria, mensagens) e linha do HARNESS actualizada; implementação na próxima iteração.
- 2026-11-27 · WH-01 · subscrição implementada (módulo webhooks: WebhookSubscriptionService só ADMIN na empresa activa, WebhookUrlValidator com https em prod e anti-SSRF após DNS por porta injectável, segredo whsec_ cifrado com AttachmentCrypto, auditoria WEBHOOK_CREATE/ACTIVATE/DEACTIVATE sem segredo, V159__webhook_subscriptions); gerador base62 partilhado em SecretTokens; WebhookSubscriptionTest 12/12 verde.
- 2026-11-27 · WH-02 · regra do corpo dos eventos escrita na SPEC §4.2.1 (origem nos eventos de domínio, envelope evt_, data por evento, customer só nome/NUIT, só subscrições activas da empresa, sem evento para estado igual ou stock que não cruza o zero) e linha do HARNESS actualizada; implementação na próxima iteração.

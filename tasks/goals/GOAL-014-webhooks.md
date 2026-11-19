---
id: GOAL-014
title: "Webhooks avisam sistemas externos de vendas, encomendas e stock"
status: TODO
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["WebhookSubscriptionTest", "WebhookEventTest", "WebhookDeliveryTest", "WebhookHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "IntegrationsUiHarnessTest"]
blockedBy: []
---

# Objectivo

O ADMIN regista um URL e escolhe eventos; quando há uma factura, nota de crédito, mudança de estado
de encomenda ou um artigo esgota/volta, o sistema envia um aviso assinado, tenta de novo se falhar e
mostra o registo de entregas. Regras em `docs/API_PUBLICA_WEBHOOKS_SPEC.md` §4.

Pré-requisito: GOAL-013 em `DONE` (ecrã de Integrações e base da API pública). Se não estiver, a
iteração termina com `NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [ ] WH-01 — subscrição: ADMIN, HTTPS, sem endereços privados, segredo uma vez — `WebhookSubscriptionTest`
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

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

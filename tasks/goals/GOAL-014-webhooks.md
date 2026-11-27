---
id: GOAL-014
title: "Webhooks avisam sistemas externos de vendas, encomendas e stock"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["WebhookSubscriptionTest", "ApiKeyRulesTest", "MigrationPostgresDialectHarnessTest", "WebhookEventTest", "OrderStatusChangedEventJpaTest", "StockAvailabilityChangedEventJpaTest", "WebhookDeliveryTest", "WebhookHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "IntegrationsUiHarnessTest"]
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
- [x] WH-02 — os quatro eventos com o corpo definido — `WebhookEventTest` · `OrderStatusChangedEventJpaTest` · `StockAvailabilityChangedEventJpaTest`
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
- 2026-11-27 · WH-02 (parcial, critério por marcar) · construção dos corpos e escolha das subscrições implementadas: eventos de domínio novos `OrderStatusChangedEvent` e `StockAvailabilityChangedEvent` (architecture.events); módulo webhooks com `WebhookEventFactory` (envelope evt_/type/occurredAt/companyId/data, dinheiro com 2 casas), `WebhookDomainEventTranslator` (lê cliente/NUIT/canal/factura por InvoiceLookup, InvoiceNoteLookup e OrderLookup; Consumidor Final sem NUIT; estado igual e stock que não cruza o zero → nada) e `WebhookEventService.prepare(...)` (só activas da empresa com o tipo, um corpo por evento, nada construído sem interessados); WebhookEventTest 12/12 verde. Falta para fechar o WH-02: o comercial publicar `OrderStatusChangedEvent` nas mudanças de estado (ComercialOrderLifecycleService, DeliveryGuide*, OrderApprovalCallback, OrderPicking*) e o inventário publicar `StockAvailabilityChangedEvent` (primeiro antes × último depois por artigo × armazém de venda na transacção).
- 2026-11-27 · WH-02 (parcial, critério por marcar) · o comercial publica `OrderStatusChangedEvent`: listener JPA único `OrderStatusChangePublisher` em `Order` (estado gravado lembrado em @PostLoad/@PostPersist, evento em @PostUpdate só quando muda de facto; criar ou regravar o mesmo estado não publica), cobrindo todos os `setStatus` (ciclo de vida, aprovação, guias, separação); OrderStatusChangedEventJpaTest 4/4 e WebhookEventTest 12/12 verdes. Falta para fechar o WH-02: o inventário publicar `StockAvailabilityChangedEvent` (primeiro antes × último depois por artigo × armazém de venda na transacção).
- 2026-11-27 · WH-02 (parcial, critério por marcar) · o inventário publica `StockAvailabilityChangedEvent`: listener JPA `StockAvailabilityPublisher` em `Stock` entrega cada linha lida/criada numa transacção de escrita ao `StockAvailabilityTracker`, que antes do commit compara a quantidade do início com a que vai ser gravada (primeiro antes × último depois; linha nova parte de zero), desconta as reservas activas (`OrderReservationLookup.activeReservation`) e publica só quando o disponível cruza o zero em armazém de venda (`Warehouse.isSalesWarehouse`, agora partilhado com a API pública) e artigo com controlo de stock; rollback não publica; StockAvailabilityChangedEventJpaTest 8/8 verde. Falta para fechar o WH-02: mudanças só de reserva (encomenda que reserva/liberta a última unidade) também cruzam o zero e devem publicar, e o "antes" tem de usar a reserva do início da transacção (facturar a última unidade reservada não é OUT_OF_STOCK).
- 2026-11-28 · WH-02 · concluído: as mudanças só de reserva também publicam `stock.availability_changed` — listener JPA `OrderReservationWatcher` (comercial) em `Order`/`OrderLine` lembra a reserva do início da transacção (encomenda activa × armazém, quantidade reservada por linha; criadas partem de zero, removidas acabam em zero) e `OrderReservationLookup.reservationChangesInCurrentTransaction()` devolve a variação líquida por artigo × armazém; o `StockAvailabilityTracker` junta essas linhas às de stock tocadas e usa no "antes" a reserva do início (facturar a última unidade reservada não é OUT_OF_STOCK); StockAvailabilityChangedEventJpaTest 12/12, OrderStatusChangedEventJpaTest 4/4 e WebhookEventTest 12/12 verdes.

---
id: GOAL-014
title: "Webhooks avisam sistemas externos de vendas, encomendas e stock"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["WebhookSubscriptionTest", "ApiKeyRulesTest", "MigrationPostgresDialectHarnessTest", "WebhookEventTest", "OrderStatusChangedEventJpaTest", "StockAvailabilityChangedEventJpaTest", "WebhookAfterCommitJpaTest", "WebhookDeliveryTest", "WebhookRetryJpaTest", "WebhookHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "IntegrationsUiHarnessTest"]
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
- [x] WH-03 — envio depois do commit, sem anular a operação — `WebhookAfterCommitJpaTest` · `WebhookEventTest`
- [x] WH-04 — assinatura HMAC e cabeçalhos — `WebhookDeliveryTest`
- [x] WH-05 — tentativas e `FAILED` — `WebhookDeliveryTest` · `WebhookRetryJpaTest`
- [x] WH-06 — registo de entregas e reenvio — `WebhookDeliveryTest`
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
- 2026-11-28 · WH-03 · regra do envio depois do commit escrita na SPEC §4.2.2 (AFTER_COMMIT, rollback → nada, entrega PENDING por subscrição em transacção nova com o corpo serializado em webhook_deliveries, despacho assíncrono por WebhookDispatcher, erros apanhados e registados sem anular a operação, janela de perda aceite) e linha do HARNESS actualizada; implementação na próxima iteração.
- 2026-11-28 · WH-03 · concluído: `WebhookEventListener` escuta os quatro eventos de domínio em AFTER_COMMIT (fallbackExecution), `WebhookEventService.prepare` lê em transacção nova e `WebhookDeliveryService.registerPending` grava uma entrega `PENDING` por subscrição (REQUIRES_NEW, corpo já serializado, attempts 0, next_attempt_at agora) em `webhook_deliveries` (V160); cada entrega vai para a porta `WebhookDispatcher` (`AsyncWebhookDispatcher` com executor próprio; o POST é do WH-04); qualquer erro é apanhado e registado em WARN sem segredo nem corpo; WebhookAfterCommitJpaTest 6/6, WebhookEventTest 12/12, StockAvailabilityChangedEventJpaTest 12/12, OrderStatusChangedEventJpaTest 4/4 e MigrationPostgresDialectHarnessTest verdes.
- 2026-11-28 · WH-04 · regra do envio escrita na SPEC §4.3.1 (WebhookSender com porta WebhookHttpClient, corpo gravado tal e qual, cabeçalhos X-Multicore-Event/Delivery/Signature, HMAC-SHA256 hex com o segredo decifrado, timeout 10 s, sem redirecções, anti-SSRF antes de ligar, 2xx → DELIVERED, falha → PENDING com tentativa registada, inactiva/não PENDING → nada) e linha do HARNESS actualizada; implementação na próxima iteração.
- 2026-11-28 · WH-04 · concluído: `WebhookSender` faz uma tentativa por entrega (lida por `WebhookDeliveryService.startAttempt` só se `PENDING` e subscrição activa; sem transacção aberta durante o HTTP): POST do corpo gravado tal e qual pela porta `WebhookHttpClient` (`JdkWebhookHttpClient`, sem redirecções, timeout 10 s), cabeçalhos Content-Type/User-Agent/X-Multicore-Event/X-Multicore-Delivery/X-Multicore-Signature (`WebhookSignature`, HMAC-SHA256 hex com o segredo decifrado), `WebhookUrlValidator` repetido antes de ligar; 2xx → `DELIVERED`, falha → `PENDING` com tentativa, código, duração e erro curto PT-MZ; `AsyncWebhookDispatcher` chama o sender no executor e apanha qualquer erro em WARN; WebhookDeliveryTest 13/13, WebhookAfterCommitJpaTest 6/6, WebhookSubscriptionTest 12/12, WebhookEventTest 12/12, CleanArchitectureHarnessTest e MulticoreServicesTest verdes.
- 2026-11-28 · WH-05 · regra das tentativas escrita na SPEC §4.3.2 (calendário +1 min/+5 min/+30 min/+2 h/+12 h a contar do fim da tentativa, 6.ª falha → FAILED, WebhookRetryPolicy com Clock injectável, WebhookRetryScheduler com PENDING vencidas de subscrições activas, máx. 50 por volta, reclamação condicional com prazo de 2 min contra envios em duplicado, inactiva espera sem contar falha) e linha do HARNESS actualizada; implementação na próxima iteração.
- 2026-11-28 · WH-05 · concluído: `WebhookRetryPolicy` (calendário único +1 min/+5 min/+30 min/+2 h/+12 h do fim da tentativa, 6.ª falha → `FAILED` com `next_attempt_at = null`, relógio injectável, prazo de reclamação 2 min); `WebhookDeliveryService.startAttempt` reclama a tentativa por UPDATE condicional (`WebhookDeliveryRepository.claim`, PENDING e vencida) e `recordFailure` agenda a próxima ou passa a `FAILED`; `WebhookRetryScheduler` (`@Scheduled`, `webhooks.retry.poll-ms` 30 000, `webhooks.retry.enabled`) despacha até 50 PENDING vencidas de subscrições activas por `next_attempt_at` (`findDueIds`), retomando as perdidas num reinício; inactiva espera sem contar falha; WebhookDeliveryTest 23/23, WebhookRetryJpaTest 3/3, WebhookAfterCommitJpaTest 6/6, CleanArchitectureHarnessTest e MulticoreServicesTest verdes.
- 2026-11-28 · WH-06 · regra do registo e reenvio escrita na SPEC §4.3.3 (listDeliveries só ADMIN/empresa activa, mais recentes primeiro, filtro por estado, página ≤ 100, sem corpo nem segredo; resend só de FAILED com subscrição activa → mesma entrega PENDING, attempts 0, despacho depois do commit, auditoria WEBHOOK_RESEND; WebhookDeliveryCleanupJob apaga entregas com mais de 30 dias em lotes) e linha do HARNESS actualizada; implementação na próxima iteração.
- 2026-11-28 · WH-06 · concluído: `WebhookDeliveryService.listDeliveries` (só ADMIN, subscrição da empresa activa, mais recentes primeiro, filtro por estado, página 50 por omissão e máx. 100, `WebhookDeliveryDTO` sem corpo nem segredo) e `resend` (só `FAILED` com subscrição activa → mesma entrega `PENDING`, `attempts = 0`, calendário recomeça, último erro mantido, auditoria `WEBHOOK_RESEND`, despacho depois do commit por `WebhookResendRequested` no `WebhookEventListener`); `WebhookDeliveryCleanupJob` (`webhooks.retention.cron` 03:30, desligável) apaga entregas com mais de 30 dias em lotes de 500; regra do ADMIN partilhada em `WebhookAdmin`; WebhookDeliveryTest 31/31, WebhookRetryJpaTest 5/5, WebhookAfterCommitJpaTest 6/6, WebhookSubscriptionTest 12/12, WebhookEventTest 12/12, CleanArchitectureHarnessTest e MulticoreServicesTest verdes.

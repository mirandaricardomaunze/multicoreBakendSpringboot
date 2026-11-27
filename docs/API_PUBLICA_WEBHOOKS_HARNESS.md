# Harness — API pública e webhooks

> Cenários que provam [API_PUBLICA_WEBHOOKS_SPEC.md](API_PUBLICA_WEBHOOKS_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-28

## API pública (GOAL-013)

| ID | Garantia | Teste |
|---|---|---|
| API-01 | Chave gerada com prefixo e 32 bytes, mostrada uma vez, guardada só em hash; revogação imediata; último uso (§3.1) — **verde 2026-11-26** | `ApiKeyRulesTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-02 | Criar/revogar chave só ADMIN, com auditoria (§3.1, §3.1.1) — **verde 2026-11-27** | `PublicApiHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| API-03 | `/api/public/v1` só por `X-Api-Key`; token de utilizador recusado aqui e chave recusada em `/api/**` (§3.2) — **verde 2026-11-26** | `PublicApiAuthenticationTest` |
| API-04 | Permissões por chave aplicadas por rota; sem permissão → 403 `API_PERMISSION_DENIED` (§3.2, §3.3) — **verde 2026-11-26** | `PublicApiAuthenticationTest` |
| API-05 | 60 pedidos/min por chave → 429 `RATE_LIMIT_EXCEEDED` com `Retry-After`; limite por chave (§3.2) — **verde 2026-11-26** | `PublicApiRateLimitTest` |
| API-06 | Erros JSON só com `code` e `message` PT-MZ, sem stack trace; `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `405 METHOD_NOT_ALLOWED`, `409 CONFLICT`, `500 INTERNAL_ERROR`; paginação máx. 100 (§3.2, §3.2.1) — **verde 2026-11-27** | `PublicApiHttpIntegrationTest` · `GlobalExceptionHandlerTest` |
| API-07 | Produtos e stock disponível da empresa da chave; sem custos, margens nem fornecedores; `updatedSince`; 404 com `code`; páginas máx. 100 (§3.3, §3.3.1) — **verde 2026-11-26** | `PublicApiCatalogTest` · `PublicApiCatalogHttpTest` |
| API-08 | Criar encomenda: preços do sistema, cliente por NUIT ou Consumidor Final com contacto, regras do desktop, origem guardada (§3.4, §3.4.1) — **verde 2026-11-27** | `PublicApiOrderTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-09 | Idempotência por chave + `externalReference`: mesmo conteúdo → 200 sem duplicar nem reservar de novo, diferente → 409 `EXTERNAL_REFERENCE_CONFLICT` (§3.4, §3.4.2) — **verde 2026-11-27** | `PublicApiOrderTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-10 | Consultar encomenda por número com estado, linhas, totais e factura; só encomendas de cliente da empresa da chave, senão 404 `ORDER_NOT_FOUND` (§3.3, §3.4.3) — **verde 2026-11-27** | `PublicApiOrderTest` |
| API-11 | OpenAPI 3 JSON só das rotas `/api/public/v1/**` em `/api/public/v1/docs`, esquema `X-Api-Key`, exige chave válida, também em prod (§3.2, §3.2.2) — **verde 2026-11-27** | `PublicApiHttpIntegrationTest` |
| API-UI-01 | Configurações › Integrações (último separador): tabela de chaves com identificação, permissões PT-MZ, último uso e estado; "Nova chave" com nome e ≥1 permissão; segredo mostrado uma vez com "Copiar" e descartado ao fechar; "Revogar" só activa e com confirmação; gerir só ADMIN (§4.4, §4.4.1) — **verde 2026-11-27** | `IntegrationsUiHarnessTest` (`apiUi01_*`) · `DesktopThinContextTest` |

## Webhooks (GOAL-014)

| ID | Garantia | Teste |
|---|---|---|
| WH-01 | Subscrição só ADMIN na empresa activa; URL + ≥1 evento; HTTPS em prod (http só fora de prod); sem utilizador/senha; endereços privados/locais recusados também após DNS; segredo `whsec_` mostrado uma vez e guardado cifrado; auditoria sem segredo (§4.1, §4.1.1) — **verde 2026-11-27** | `WebhookSubscriptionTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| WH-02 | Eventos `invoice.issued`, `credit_note.issued`, `order.status_changed`, `stock.availability_changed` a partir dos eventos de domínio; envelope `id`/`type`/`occurredAt`/`companyId`/`data`; `data` por evento sem dados pessoais além de nome/NUIT; só subscrições activas da empresa com o evento; estado igual ou stock que não cruza o zero não gera evento (§4.2, §4.2.1) — **verde 2026-11-28** (corpos, `order.status_changed` pelo comercial, `stock.availability_changed` por quantidade e por reserva, com a reserva do início da transacção no "antes") | `WebhookEventTest` · `OrderStatusChangedEventJpaTest` · `StockAvailabilityChangedEventJpaTest` |
| WH-03 | Depois do commit (rollback → nada), em transacção nova, uma entrega `PENDING` por subscrição interessada com o corpo já serializado em `webhook_deliveries`; despacho assíncrono fora da thread do negócio; qualquer erro do webhook é apanhado e registado sem anular a operação (§4.2, §4.2.2) — **verde 2026-11-28** | `WebhookAfterCommitJpaTest` · `WebhookEventTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| WH-04 | Uma tentativa de envio por `WebhookSender`: `POST` com o corpo gravado tal e qual, `Content-Type`/`User-Agent`, `X-Multicore-Event`, `X-Multicore-Delivery` (id da entrega, estável), `X-Multicore-Signature: sha256=<hex>` (HMAC-SHA256 do corpo com o segredo decifrado); timeout 10 s, sem redirecções, anti-SSRF repetido antes de ligar; 2xx → `DELIVERED`, falha → `PENDING` com `attempts`/código/duração/erro curto; subscrição inactiva ou entrega não `PENDING` → nenhum pedido; segredo nunca em log (§4.3, §4.3.1) — **verde 2026-11-28** | `WebhookDeliveryTest` |
| WH-05 | Falhas seguidas agendam nova tentativa a +1 min, +5 min, +30 min, +2 h, +12 h do fim de cada tentativa (`WebhookRetryPolicy`, relógio injectável); a 6.ª falha → `FAILED` com `next_attempt_at = null`; `WebhookRetryScheduler` despacha só `PENDING` vencidas de subscrições activas, máx. 50 por volta, e retoma as que ficaram após reinício; tentativa reclamada por actualização condicional (prazo 2 min) nunca é enviada em duplicado; inactiva → espera sem contar falha; timeout 10 s (§4.3, §4.3.2) — **verde 2026-11-28** | `WebhookDeliveryTest` · `WebhookRetryJpaTest` |
| WH-06 | Registo de entregas por subscrição (só ADMIN, empresa activa, mais recentes primeiro, filtro por estado, paginado ≤ 100, sem corpo nem segredo); reenvio manual só de `FAILED` com subscrição activa → mesma entrega `PENDING`, `attempts = 0`, despacho depois do commit, auditoria `WEBHOOK_RESEND`; `WebhookDeliveryCleanupJob` apaga entregas com mais de 30 dias (§4.3, §4.3.3) — **regra escrita 2026-11-28** | `WebhookDeliveryTest` |
| WH-07 | 20 falhas seguidas desactivam e avisam o ADMIN (§4.3) | `WebhookDeliveryTest` |
| WH-08 | Endpoints de subscrições e entregas; ADMIN; empresa activa (§4) | `WebhookHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` · `PostgresSchemaParityHarnessTest` |
| WH-UI-01 | Configurações › Integrações: webhooks, eventos, registo e reenviar (§4.4) | `IntegrationsUiHarnessTest` |

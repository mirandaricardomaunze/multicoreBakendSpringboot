# Harness — API pública e webhooks

> Cenários que provam [API_PUBLICA_WEBHOOKS_SPEC.md](API_PUBLICA_WEBHOOKS_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-27

## API pública (GOAL-013)

| ID | Garantia | Teste |
|---|---|---|
| API-01 | Chave gerada com prefixo e 32 bytes, mostrada uma vez, guardada só em hash; revogação imediata; último uso (§3.1) — **verde 2026-11-26** | `ApiKeyRulesTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-02 | Criar/revogar chave só ADMIN, com auditoria (§3.1, §3.1.1) — **verde 2026-11-27** | `PublicApiHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` |
| API-03 | `/api/public/v1` só por `X-Api-Key`; token de utilizador recusado aqui e chave recusada em `/api/**` (§3.2) — **verde 2026-11-26** | `PublicApiAuthenticationTest` |
| API-04 | Permissões por chave aplicadas por rota; sem permissão → 403 `API_PERMISSION_DENIED` (§3.2, §3.3) — **verde 2026-11-26** | `PublicApiAuthenticationTest` |
| API-05 | 60 pedidos/min por chave → 429 `RATE_LIMIT_EXCEEDED` com `Retry-After`; limite por chave (§3.2) — **verde 2026-11-26** | `PublicApiRateLimitTest` |
| API-06 | Erros JSON com `code` e `message` PT-MZ, sem stack trace; paginação máx. 100 (§3.2) | `PublicApiHttpIntegrationTest` |
| API-07 | Produtos e stock disponível da empresa da chave; sem custos, margens nem fornecedores; `updatedSince`; 404 com `code`; páginas máx. 100 (§3.3, §3.3.1) — **verde 2026-11-26** | `PublicApiCatalogTest` · `PublicApiCatalogHttpTest` |
| API-08 | Criar encomenda: preços do sistema, cliente por NUIT ou Consumidor Final com contacto, regras do desktop, origem guardada (§3.4, §3.4.1) — **verde 2026-11-27** | `PublicApiOrderTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-09 | Idempotência por chave + `externalReference`: mesmo conteúdo → 200 sem duplicar nem reservar de novo, diferente → 409 `EXTERNAL_REFERENCE_CONFLICT` (§3.4, §3.4.2) — **verde 2026-11-27** | `PublicApiOrderTest` · `PostgresSchemaParityHarnessTest` · `MigrationPostgresDialectHarnessTest` |
| API-10 | Consultar encomenda por número com estado, linhas, totais e factura; só encomendas de cliente da empresa da chave, senão 404 `ORDER_NOT_FOUND` (§3.3, §3.4.3) — **verde 2026-11-27** | `PublicApiOrderTest` |
| API-11 | OpenAPI só do grupo público em `/api/public/v1/docs` (§3.2) | `PublicApiHttpIntegrationTest` |
| API-UI-01 | Configurações › Integrações: chaves com permissões, mostrar uma vez, revogar, último uso (§4.4) | `IntegrationsUiHarnessTest` |

## Webhooks (GOAL-014)

| ID | Garantia | Teste |
|---|---|---|
| WH-01 | Subscrição só ADMIN; HTTPS em prod; endereços privados recusados; segredo mostrado uma vez (§4.1) | `WebhookSubscriptionTest` |
| WH-02 | Eventos `invoice.issued`, `credit_note.issued`, `order.status_changed`, `stock.availability_changed` com corpo definido (§4.2) | `WebhookEventTest` |
| WH-03 | Envio só depois do commit; falha do webhook não anula a operação (§4.2) | `WebhookEventTest` |
| WH-04 | Assinatura HMAC-SHA256 e cabeçalhos de evento e entrega (§4.3) | `WebhookDeliveryTest` |
| WH-05 | Tentativas a 1 min, 5 min, 30 min, 2 h, 12 h; depois `FAILED`; timeout 10 s (§4.3) | `WebhookDeliveryTest` |
| WH-06 | Registo de entregas 30 dias; reenviar `FAILED` (§4.3) | `WebhookDeliveryTest` |
| WH-07 | 20 falhas seguidas desactivam e avisam o ADMIN (§4.3) | `WebhookDeliveryTest` |
| WH-08 | Endpoints de subscrições e entregas; ADMIN; empresa activa (§4) | `WebhookHttpIntegrationTest` · `ApiRolePolicyEndpointCoverageHarnessTest` · `PostgresSchemaParityHarnessTest` |
| WH-UI-01 | Configurações › Integrações: webhooks, eventos, registo e reenviar (§4.4) | `IntegrationsUiHarnessTest` |

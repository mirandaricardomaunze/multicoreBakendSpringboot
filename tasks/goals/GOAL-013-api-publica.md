---
id: GOAL-013
title: "Sistemas externos lêem o catálogo e criam encomendas pela API pública"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["ApiKeyRulesTest", "PublicApiAuthenticationTest", "PublicApiRateLimitTest", "PublicApiCatalogTest", "PublicApiOrderTest", "PublicApiHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "IntegrationsUiHarnessTest"]
blockedBy: []
---

# Objectivo

O ADMIN cria uma chave de API para a loja online ou o contabilista; com ela, um sistema externo lê
artigos, preços e stock disponível e cria encomendas que aparecem no desktop para facturar.
Regras em `docs/API_PUBLICA_WEBHOOKS_SPEC.md` §2–§3 e §4.4.

## Critérios de aceitação

- [x] API-01 — chave gerada, mostrada uma vez, guardada em hash, revogável — `ApiKeyRulesTest`
- [x] API-03 — autenticação só por `X-Api-Key`, separada da API interna — `PublicApiAuthenticationTest`
- [x] API-04 — permissões por chave — `PublicApiAuthenticationTest`
- [ ] API-05 — limite 60/min com 429 — `PublicApiRateLimitTest`
- [ ] API-07 — catálogo e stock disponível sem dados internos — `PublicApiCatalogTest`
- [ ] API-08 — criar encomenda com as regras do desktop — `PublicApiOrderTest`
- [ ] API-09 — idempotência por `externalReference` — `PublicApiOrderTest`
- [ ] API-10 — consultar encomenda — `PublicApiOrderTest`
- [ ] API-02 — criar/revogar só ADMIN com auditoria — `PublicApiHttpIntegrationTest`
- [ ] API-06 — erros e paginação — `PublicApiHttpIntegrationTest`
- [ ] API-11 — OpenAPI do grupo público — `PublicApiHttpIntegrationTest`
- [ ] API-UI-01 — ecrã de chaves de API — `IntegrationsUiHarnessTest`

## Fora de âmbito

- Webhooks (GOAL-014); OAuth, escrita de catálogo, facturar pela API (SPEC §5).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · API-01 · módulo `publicapi`: chave `mk_live_` + 32 bytes base62 mostrada só na criação, guardada em hash SHA-256 com os 8 caracteres de identificação, revogação imediata, último uso registado, auditoria `API_KEY_CREATE`/`API_KEY_REVOKE`, migração V155 `api_keys`; ApiKeyRulesTest verde 9/9.
- 2026-11-26 · API-03 · `ApiKeyAuthenticationFilter` + `PublicApiRoutes`: `/api/public/**` só abre com `X-Api-Key` (empresa da chave, sem `X-Company-Id`), token de utilizador → 401 `USER_TOKEN_NOT_ACCEPTED`, chave em falta/revogada → 401 JSON `API_KEY_REQUIRED`/`API_KEY_INVALID`, chave recusada no `/api/**` interno; interceptores do desktop excluídos da rota pública; PublicApiAuthenticationTest verde 6/6.
- 2026-11-26 · API-04 · `PublicApiPermissions` (rota+método → permissão, §3.3) verificado no `ApiKeyAuthenticationFilter` antes do recurso: chave sem a permissão → 403 JSON `API_PERMISSION_DENIED` com a permissão em falta, `ORDERS_WRITE` não dá `ORDERS_READ`; regra do 403 escrita na SPEC §3.3; PublicApiAuthenticationTest verde 9/9.

---
id: GOAL-013
title: "Sistemas externos lêem o catálogo e criam encomendas pela API pública"
status: IN_PROGRESS
priority: MEDIUM
spec: docs/API_PUBLICA_WEBHOOKS_SPEC.md
harness: docs/API_PUBLICA_WEBHOOKS_HARNESS.md
verify: ["ApiKeyRulesTest", "PublicApiAuthenticationTest", "PublicApiRateLimitTest", "PublicApiCatalogTest", "PublicApiCatalogHttpTest", "PublicApiOrderTest", "PublicApiHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "IntegrationsUiHarnessTest"]
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
- [x] API-05 — limite 60/min com 429 — `PublicApiRateLimitTest`
- [x] API-07 — catálogo e stock disponível sem dados internos — `PublicApiCatalogTest` · `PublicApiCatalogHttpTest`
- [x] API-08 — criar encomenda com as regras do desktop — `PublicApiOrderTest`
- [ ] API-09 — idempotência por `externalReference` — `PublicApiOrderTest`
- [ ] API-10 — consultar encomenda — `PublicApiOrderTest`
- [ ] API-02 — criar/revogar só ADMIN com auditoria — `PublicApiHttpIntegrationTest`
- [ ] API-06 — erros e paginação — `PublicApiHttpIntegrationTest`
- [ ] API-11 — OpenAPI do grupo público — `PublicApiHttpIntegrationTest`
- [ ] API-UI-01 — ecrã de chaves de API — `IntegrationsUiHarnessTest`

## Fora de âmbito

- Webhooks (GOAL-014); OAuth, escrita de catálogo, facturar pela API (SPEC §5).

## Perguntas em aberto

**P1 (API-08) — Por que via entra a encomenda criada pela API?** A SPEC §3.4 pede "stock disponível, reserva,
limite de crédito, fluxo de aprovação" e "estado inicial o mesmo que uma encomenda nova do desktop", mas o
desktop tem duas vias e nenhuma faz as quatro coisas:
- `FORMAL_ORDER` (Encomenda A4, a via por omissão): nasce `PENDING_APPROVAL`, passa pelo motor de aprovações,
  **não** verifica stock nem reserva na criação; o stock e o crédito só se verificam na facturação (ou na aprovação).
- `PICKING_REQUEST` (Pedido de separação): verifica stock disponível e **reserva** logo, nasce
  `AWAITING_SEPARATION`, **sem** aprovação; o crédito é verificado na facturação.

Opções:
- **A (recomendada)** — `FORMAL_ORDER` com aprovação e estado `PENDING_APPROVAL` como no desktop, **mais**
  verificação de stock disponível e reserva activa na criação (a loja online não pode vender o que já está
  prometido; a reserva liberta-se ao facturar/cancelar como hoje). É uma combinação nova, que só a API usa.
- B — `PICKING_REQUEST` tal como o desktop (reserva, vai directo para separação no armazém, sem aprovação).
- C — `FORMAL_ORDER` exactamente como no desktop (aprovação, sem reserva; stock só na facturação).

**P2 (API-08) — Nome do cliente novo criado por NUIT.** O corpo da SPEC só tem `nuit`, `contactName` e
`contactPhone`; o registo manual de cliente exige nome. Recomendação: campo opcional `customerName`, obrigatório
só quando o NUIT ainda não existe na empresa (sem ele → 400 `CUSTOMER_NAME_REQUIRED`); alternativa: usar o
`contactName` como nome do cliente.

**Resposta do responsável (2026-11-27):** P1 → opção **A** (`FORMAL_ORDER` com aprovação e reserva activa na
criação); P2 → `customerName` opcional, obrigatório quando o NUIT é novo (`400 CUSTOMER_NAME_REQUIRED`).
Regras escritas na SPEC §3.4.

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · API-01 · módulo `publicapi`: chave `mk_live_` + 32 bytes base62 mostrada só na criação, guardada em hash SHA-256 com os 8 caracteres de identificação, revogação imediata, último uso registado, auditoria `API_KEY_CREATE`/`API_KEY_REVOKE`, migração V155 `api_keys`; ApiKeyRulesTest verde 9/9.
- 2026-11-26 · API-03 · `ApiKeyAuthenticationFilter` + `PublicApiRoutes`: `/api/public/**` só abre com `X-Api-Key` (empresa da chave, sem `X-Company-Id`), token de utilizador → 401 `USER_TOKEN_NOT_ACCEPTED`, chave em falta/revogada → 401 JSON `API_KEY_REQUIRED`/`API_KEY_INVALID`, chave recusada no `/api/**` interno; interceptores do desktop excluídos da rota pública; PublicApiAuthenticationTest verde 6/6.
- 2026-11-26 · API-04 · `PublicApiPermissions` (rota+método → permissão, §3.3) verificado no `ApiKeyAuthenticationFilter` antes do recurso: chave sem a permissão → 403 JSON `API_PERMISSION_DENIED` com a permissão em falta, `ORDERS_WRITE` não dá `ORDERS_READ`; regra do 403 escrita na SPEC §3.3; PublicApiAuthenticationTest verde 9/9.
- 2026-11-26 · API-05 · `PublicApiRateLimiter` (janela fixa de 1 min por chave, recusados não contam) aplicado no `ApiKeyAuthenticationFilter` depois da autenticação: o 61.º pedido → 429 JSON `RATE_LIMIT_EXCEEDED` com `Retry-After` em segundos (≥ 1); uma chave esgotada não trava outra; regra escrita na SPEC §3.2; PublicApiRateLimitTest verde 3/3.
- 2026-11-26 · API-07 · `PublicCatalogController` + `PublicCatalogService`: `GET /products`, `/products/{sku}` e `/stock` da empresa da chave, só dados de venda (preço com IVA pelo `LineCalculator`, taxa em %, unidade base, `updatedAt` com fuso), sem custo/grosso/margens/fornecedores; `updatedSince` ISO 8601; stock disponível = quantidade − reservas activas (porta nova `OrderReservationLookup`) nos armazéns de venda, nunca negativo; 404 `PRODUCT_NOT_FOUND`/`WAREHOUSE_NOT_FOUND`; páginas a partir de 0, máx. 100; regra escrita na SPEC §3.3.1; PublicApiCatalogTest 7/7 e PublicApiCatalogHttpTest 2/2 verdes.
- 2026-11-26 · API-08 · bloqueado: a via da encomenda (aprovação vs reserva) e o nome do cliente novo por NUIT não estão na SPEC §3.4; perguntas P1/P2 em `## Perguntas em aberto`.
- 2026-11-27 · API-08 · `POST /api/public/v1/orders` (`PublicOrderController` + `PublicOrderService`): a chave actua como vendedor na sua empresa (`CurrentUserContext.runAs`), SKU/armazém de venda traduzidos, preço e IVA do sistema; nasce `FORMAL_ORDER` com o motor de aprovações e reserva o stock livre na criação (`ExternalOrderPlacementService`, `OrderStockAvailability.requireAndReserve`), sem stock → 422 `INSUFFICIENT_STOCK` sem gravar nada; cliente por NUIT (novo exige `customerName` → 400 `CUSTOMER_NAME_REQUIRED`) ou Consumidor Final com contacto (400 `CONTACT_REQUIRED`); origem `API` + nome da chave, referência externa e telefone gravados (V157); rejeição na aprovação liberta a reserva; detalhe na SPEC §3.4.1; PublicApiOrderTest 6/6 verde.

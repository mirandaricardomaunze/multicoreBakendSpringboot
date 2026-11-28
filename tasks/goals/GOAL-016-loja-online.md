---
id: GOAL-016
title: "Clientes encomendam pela loja online e levantam ou recebem em casa"
status: IN_PROGRESS
priority: LOW
spec: docs/LOJA_ONLINE_SPEC.md
harness: docs/LOJA_ONLINE_HARNESS.md
verify: ["OnlineShopSettingsTest", "OnlineShopCatalogTest", "OnlineShopOrderTest", "OnlineShopExpiryTest", "OnlineShopHttpIntegrationTest", "OnlineShopStaticAssetsHarnessTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "OnlineShopUiHarnessTest"]
blockedBy: []
---

# Objectivo

A empresa activa a loja em `/loja/{código}`; o cliente vê os artigos publicados com
Disponível/Esgotado, encomenda com nome e telemóvel, escolhe levantamento ou entrega, e paga ao
receber. A encomenda aparece no desktop para facturar. Regras em `docs/LOJA_ONLINE_SPEC.md`.

Pré-requisito: GOAL-013 em `DONE` (serviço de criação de encomendas da API pública). Se não estiver,
a iteração termina com `NEEDS_HUMAN` e não começa este goal.

## Critérios de aceitação

- [x] LO-01 — activação e definições da loja — `OnlineShopSettingsTest`
- [x] LO-02 — só artigos publicados — `OnlineShopCatalogTest`
- [x] LO-03 — Disponível/Esgotado sem dados internos — `OnlineShopCatalogTest`
- [x] LO-05 — validação do checkout — `OnlineShopOrderTest`
- [x] LO-06 — encomenda pelo serviço da API pública — `OnlineShopOrderTest`
- [x] LO-07 — escolha do armazém e recusa sem stock — `OnlineShopOrderTest`
- [x] LO-08 — taxa de entrega por zona — `OnlineShopOrderTest`
- [x] LO-10 — cancelamento automático — `OnlineShopExpiryTest`
- [x] LO-04 — endpoints públicos, limites e honeypot — `OnlineShopHttpIntegrationTest`
- [x] LO-09 — consulta por número + telemóvel — `OnlineShopHttpIntegrationTest`
- [x] LO-11 — página pública segura e móvel — `OnlineShopStaticAssetsHarnessTest`
- [x] LO-12 — modo, zona/loja e morada guardados na encomenda e no `OrderDTO` — `OnlineShopOrderTest`
- [ ] LO-UI-01 — definições da loja em Configurações › Integrações — `OnlineShopUiHarnessTest`
- [ ] LO-UI-02 — publicar artigo na ficha do artigo — `OnlineShopUiHarnessTest`
- [ ] LO-UI-03 — encomendas marcadas "Loja online" e alerta no sino — `OnlineShopUiHarnessTest`

## Fora de âmbito

- Pagamento online, contas de cliente, cupões, domínio próprio, SMS (SPEC §4).

## Perguntas em aberto

**P1 — Pré-requisito GOAL-013 por cumprir.** LO-06 manda criar a encomenda "pelo mesmo serviço da API
pública" (`docs/API_PUBLICA_WEBHOOKS_SPEC.md` §3.4), mas o GOAL-013 está `BLOCKED` em API-08 (via da
encomenda: aprovação vs reserva; nome do cliente novo por NUIT — perguntas P1/P2 do GOAL-013). O goal manda
terminar com `NEEDS_HUMAN` sem começar.

Opções:
- **A (recomendada)** — responder às perguntas P1/P2 do GOAL-013; quando o GOAL-013 ficar `DONE`, este goal
  passa a `TODO` e o loop retoma em LO-01.
- B — autorizar começar já os critérios que não dependem da API pública (LO-01, LO-02, LO-03) e deixar
  LO-06/07/08 para depois do GOAL-013.

**Resposta do responsável (2026-11-27):** opção **A** — P1/P2 do GOAL-013 respondidas; este goal começa
em LO-01 quando o GOAL-013 estiver `DONE`.

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->
- 2026-11-26 · pré-requisito · bloqueado: GOAL-013 está `BLOCKED` (API-08), não `DONE`; goal não iniciado, pergunta P1 em `## Perguntas em aberto`.
- 2026-11-28 · LO-01 · concluído: OnlineShopSettingsService (só ADMIN) grava a loja da empresa com código público único (minúsculas, números, hífen), lojas de levantamento entre os armazéns de venda, zonas com taxa ≥ 0 e prazo (omissão 7), com auditoria ONLINE_SHOP_SETTINGS; migração V162; OnlineShopSettingsTest 9/9 verde.
- 2026-11-28 · LO-02 · concluído: campo "Publicar na loja online" no artigo (desligado por omissão, migração V163); OnlineShopCatalogService lista só os artigos publicados da loja activa (nunca o Adiantamento), loja desactivada ou inexistente → "Loja indisponível.", publicar/retirar auditado (ONLINE_SHOP_PUBLISH); OnlineShopCatalogTest 6/6 verde.
- 2026-11-28 · LO-03 · concluído: cada artigo da montra traz só Disponível/Esgotado (`available`), Disponível quando stock − reservas activas > 0 em pelo menos uma loja de levantamento (serviços sem controlo de stock sempre disponíveis; stock noutros armazéns não conta), lido pelo StockLookup e OrderReservationLookup; o DTO não tem quantidades, custos nem fornecedores; OnlineShopCatalogTest 11/11 verde.
- 2026-11-28 · LO-05 · concluído: OnlineShopCheckoutRules valida o checkout (PlaceOnlineShopOrderRequest): nome obrigatório (≤150), telemóvel moçambicano normalizado como no pagamento móvel (258 opcional, 9 dígitos, 82–87), NUIT opcional mas válido (módulo 11), modo LEVANTAMENTO numa loja de levantamento da loja ou ENTREGA com zona da loja e morada (≤255), notas ≤500 e carrinho não vazio com quantidades > 0; OnlineShopOrderTest 10/10 verde.
- 2026-11-28 · LO-06 · concluído: OnlineShopOrderService cria a encomenda pelo ExternalOrderPlacementService da API pública (Encomenda A4 em aprovação com reserva, preços e IVA do sistema, só artigos publicados), cliente por NUIT (registado com o nome do checkout quando novo) ou Consumidor Final, telemóvel guardado na encomenda, nova origem `LOJA_ONLINE` (ExternalOrderSource passa a levar a origem), a loja actua como vendedor "Loja online · {código}"; o nome do checkout passa a ≤120 (limite do comprador na encomenda); a resposta traz só número, estado, resumo e total; OnlineShopOrderTest 16/16 verde.
- 2026-11-28 · LO-07 · concluído: o armazém da encomenda é a loja de levantamento escolhida (sem stock livre para algum artigo → "Sem stock suficiente na loja escolhida para: …" com a lista dos artigos) ou, na entrega, a primeira loja de levantamento com stock livre (stock − reservas, quantidades somadas por artigo, serviços sem controlo de stock dispensados) para todo o carrinho, senão "Não há stock suficiente para entrega."; nada é criado quando recusa; cálculo do stock livre partilhado com a montra (PickupFreeStock); OnlineShopOrderTest 24/24 verde.
- 2026-11-28 · LO-08 · concluído: a entrega acrescenta à encomenda uma linha de serviço "Taxa de entrega — {zona}" (1 × taxa da zona, IVA normal, preço do artigo e nunca do pedido); o artigo de serviço por zona (SKU `LOJA-ENTREGA-{empresa}-{zona}`, sem stock, nunca publicado, sem taxa própria) é criado pelo DeliveryFeeProductProvisioner na activação da loja e acompanha o nome e a taxa da zona a cada gravação e no checkout; levantamento sem taxa; OnlineShopOrderTest 29/29 e OnlineShopSettingsTest 11/11 verdes.
- 2026-11-28 · LO-10 · concluído: OnlineShopExpiryService (corrido de hora a hora pelo OnlineShopExpiryJob, `onlineshop.expiry.*`) cancela, como SYSTEM da empresa de cada loja (mesmo desactivada), as encomendas `LOJA_ONLINE` ainda canceláveis criadas há mais do que o prazo da loja, pelo mesmo cancelamento do desktop (ComercialService.cancelOrder: fecha a aprovação pendente, liberta a reserva e audita ORDER_CANCEL com o motivo "Cancelada automaticamente: encomenda da loja online não facturada em N dia(s)."); uma encomenda recusada não trava as outras; consulta nova OrderLookup.listCancellableByOriginCreatedBefore; OnlineShopExpiryTest 6/6 verde.
- 2026-11-28 · LO-04 · concluído: OnlineShopPublicController expõe `/api/shop/{código}/products` e `POST /api/shop/{código}/orders` sem login (permitAll, fora dos interceptores de empresa/versão, área `/api/shop` classificada no ApiRolePolicy); loja desactivada ou inexistente → "Loja indisponível."; OnlineShopRateLimitFilter limita por IP (ClientIpResolver) a 60 pedidos/min e 5 tentativas de encomenda/hora com 429 `RATE_LIMIT_EXCEEDED` e `Retry-After` (janela fixa partilhada com a API pública: FixedWindowRateLimiter); campo-armadilha `website` no checkout recusa com "Não foi possível registar a encomenda." antes de procurar a loja, sem criar nada; OnlineShopHttpIntegrationTest 6/6 verde.
- 2026-11-28 · LO-09 · concluído: `POST /api/shop/{código}/orders/lookup` (número + telemóvel no corpo, fora dos logs e do limite horário de encomendas) devolve número, estado, linhas e total só das encomendas `LOJA_ONLINE` da empresa da loja com esse telemóvel (normalizado como no checkout); telemóvel errado ou inválido, número inexistente ou em falta respondem todos "Encomenda não encontrada." (OnlineShopOrderTrackingService sobre CustomerOrderTrackingService.findByNumberAndContactPhone); OnlineShopHttpIntegrationTest 9/9 verde.
- 2026-11-28 · LO-11 · concluído: página pública `/loja/{código}` (forward para `static/loja/index.html`, código só minúsculas/números/hífen) com loja.css, loja.js e icon.svg próprios, sem CDN nem script/estilo/handlers inline, CSP só 'self' + DENY + no-referrer (PortalSecurityHeaders passa a cobrir `/loja`), texto da API só por `textContent`, layout a partir de 360 px, catálogo por categoria com pesquisa e Disponível/Esgotado (esgotado não se adiciona), carrinho no `localStorage` só com SKU + quantidade, checkout com campo-armadilha escondido e consulta por número + telemóvel, valores em 1.460,00 MT; novo `GET /api/shop/{código}` (OnlineShopStorefrontService) com nome, contacto, lojas de levantamento e zonas com taxa; OnlineShopStaticAssetsHarnessTest 11/11 verde.
- 2026-11-28 · LO-UI-01 · spec: critério demasiado grande e sem regra para guardar a entrega (a morada do checkout perdia-se); SPEC §3.5 escrita (fulfilmentLabel e deliveryAddress na encomenda, secção "Loja online" em Integrações, caixa "Publicar na loja online" na ficha do artigo, marca "Loja online" e alerta no sino) e critério dividido em LO-12, LO-UI-01, LO-UI-02 e LO-UI-03 no HARNESS e no goal; sem código.
- 2026-11-28 · LO-12 · concluído: a encomenda `LOJA_ONLINE` guarda `fulfilmentLabel` ("Levantamento — {loja}" pelo WarehouseLookup ou "Entrega — {zona}", ≤120) e `deliveryAddress` (só na entrega) através do ExternalOrderSource do serviço da API pública (migração V164, colunas em customer_orders); o `OrderDTO` passa a trazer `origin`, `originLabel` (Desktop / API · {chave} / Loja online, OrderOrigin.label), `contactPhone`, `fulfilmentLabel` e `deliveryAddress`, com o construtor antigo a valer como desktop; desktop e API ficam nulos; OnlineShopOrderTest 35/35 verde.

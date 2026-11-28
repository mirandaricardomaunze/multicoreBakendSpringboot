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
- [ ] LO-05 — validação do checkout — `OnlineShopOrderTest`
- [ ] LO-06 — encomenda pelo serviço da API pública — `OnlineShopOrderTest`
- [ ] LO-07 — escolha do armazém e recusa sem stock — `OnlineShopOrderTest`
- [ ] LO-08 — taxa de entrega por zona — `OnlineShopOrderTest`
- [ ] LO-10 — cancelamento automático — `OnlineShopExpiryTest`
- [ ] LO-04 — endpoints públicos, limites e honeypot — `OnlineShopHttpIntegrationTest`
- [ ] LO-09 — consulta por número + telemóvel — `OnlineShopHttpIntegrationTest`
- [ ] LO-11 — página pública segura e móvel — `OnlineShopStaticAssetsHarnessTest`
- [ ] LO-UI-01 — definições, publicar artigo e encomendas online no desktop — `OnlineShopUiHarnessTest`

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

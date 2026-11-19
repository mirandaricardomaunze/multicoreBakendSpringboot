---
id: GOAL-016
title: "Clientes encomendam pela loja online e levantam ou recebem em casa"
status: TODO
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

- [ ] LO-01 — activação e definições da loja — `OnlineShopSettingsTest`
- [ ] LO-02 — só artigos publicados — `OnlineShopCatalogTest`
- [ ] LO-03 — Disponível/Esgotado sem dados internos — `OnlineShopCatalogTest`
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

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

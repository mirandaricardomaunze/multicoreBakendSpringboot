---
id: GOAL-018
title: "Cliente levanta material já pago mesmo sem internet no estaleiro"
status: TODO
priority: MEDIUM
spec: docs/LEVANTAMENTO_CONTINGENCIA_SPEC.md
harness: docs/LEVANTAMENTO_CONTINGENCIA_HARNESS.md
verify: ["PickupOfflineCacheTest", "PickupContingencyQueueTest", "PickupContingencySyncTest", "PickupContingencyIncidentTest", "PickupContingencyHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "PrintableTextHarnessTest", "PickupContingencyUiHarnessTest"]
blockedBy: []
---

# Objectivo

Quando a internet cai, o balcão do estaleiro emite um talão de levantamento provisório limitado ao último
saldo conhecido; quando a ligação volta, o talão vira LV normal ou, se não puder ser aceite, vai para o
gestor resolver. Regras em `docs/LEVANTAMENTO_CONTINGENCIA_SPEC.md` §3 (decididas pelo responsável em 2026-11-28).

## Critérios de aceitação

- [ ] LC2-01 — saldos guardados no computador — `PickupOfflineCacheTest`
- [ ] LC2-02 — regras do talão provisório e limite pelo saldo guardado — `PickupContingencyQueueTest`
- [ ] LC2-03 — fila local durável — `PickupContingencyQueueTest`
- [ ] LC2-04 — impressão do talão provisório no desktop — `PickupContingencyQueueTest`
- [ ] LC2-05 — servidor aceita o talão sincronizado sem duplicar — `PickupContingencySyncTest`
- [ ] LC2-06 — servidor grava incidente "Para rever" na recusa — `PickupContingencySyncTest`
- [ ] LC2-07 — sincronização em segundo plano e manual — `PickupContingencyQueueTest`
- [ ] LC2-08 — gestor aceita, cobra a diferença ou anula — `PickupContingencyIncidentTest`
- [ ] LC2-09 — endpoints, perfis e empresa activa — `PickupContingencyHttpIntegrationTest`
- [ ] LC2-UI-01 — ecrã Material por Levantar sem ligação — `PickupContingencyUiHarnessTest`
- [ ] LC2-UI-02 — ecrã de incidentes para a gestão — `PickupContingencyUiHarnessTest`

## Fora de âmbito

- Login sem internet; guias, faturas e produção sem internet; stock (SPEC §4).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

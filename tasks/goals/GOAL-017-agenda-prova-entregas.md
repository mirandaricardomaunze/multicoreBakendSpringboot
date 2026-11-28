---
id: GOAL-017
title: "Entregas agendadas por dia e camião, com prova de entrega e devolução na obra"
status: TODO
priority: MEDIUM
spec: docs/ENTREGAS_AGENDA_PROVA_SPEC.md
harness: docs/ENTREGAS_AGENDA_PROVA_HARNESS.md
verify: ["DeliveryScheduleRulesTest", "DeliveryProofTest", "SiteReturnTest", "DeliveryScheduleHttpIntegrationTest", "ApiRolePolicyEndpointCoverageHarnessTest", "PostgresSchemaParityHarnessTest", "MigrationPostgresDialectHarnessTest", "DeliveryAgendaUiHarnessTest"]
blockedBy: []
---

# Objectivo

O estaleiro sabe que entregas há em cada dia e em que camião, guarda a prova de quem recebeu o material
e, quando sobra material na obra, o stock volta ao chegar ao armazém e a nota de crédito fica pronta para
o gestor aprovar. Regras em `docs/ENTREGAS_AGENDA_PROVA_SPEC.md` §3 (decididas pelo responsável em 2026-11-28).

## Critérios de aceitação

- [ ] EN-01 — agendar guia aprovada com dia, período e camião; estados da entrega — `DeliveryScheduleRulesTest`
- [ ] EN-02 — não entregue com motivo, reagendar, sem stock — `DeliveryScheduleRulesTest`
- [ ] EN-03 — agenda do dia por camião e período — `DeliveryScheduleRulesTest`
- [ ] EN-04 — prova de entrega: nome obrigatório, assinatura e foto opcionais — `DeliveryProofTest`
- [ ] EN-05 — prova no PDF da guia — `DeliveryProofTest`
- [ ] EN-06 — registar devolução na obra sem mexer no stock — `SiteReturnTest`
- [ ] EN-07 — receber devolução no armazém repõe o stock — `SiteReturnTest`
- [ ] EN-08 — guia faturada: nota de crédito pendente sem segunda devolução de stock — `SiteReturnTest`
- [ ] EN-09 — guia por faturar: fatura só o líquido entregue — `SiteReturnTest`
- [ ] EN-10 — endpoints, perfis e empresa activa — `DeliveryScheduleHttpIntegrationTest`
- [ ] EN-UI-01 — acções agendar, registar entrega e não entregue — `DeliveryAgendaUiHarnessTest`
- [ ] EN-UI-02 — acções devolução na obra e receber devolução — `DeliveryAgendaUiHarnessTest`
- [ ] EN-UI-03 — separador Agenda de Entregas — `DeliveryAgendaUiHarnessTest`

## Fora de âmbito

- App de telemóvel do motorista, GPS, hora exacta, frete e manifestos (SPEC §4).

## Perguntas em aberto

(Vazio. A IA escreve aqui quando precisa de decisão humana e passa o goal a BLOCKED.)

## Registo de progresso

<!-- uma linha por iteração: data · critério · resultado -->

# Harness — Agenda de entregas, prova de entrega e devolução na obra

> Cenários que provam [ENTREGAS_AGENDA_PROVA_SPEC.md](ENTREGAS_AGENDA_PROVA_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-28 (EN-01 verde: `DeliveryScheduleRulesTest`, migração V165)

| ID | Garantia | Teste |
|---|---|---|
| EN-01 | Só guia `APPROVED` se agenda; dia não anterior a hoje; período `MORNING`/`AFTERNOON`; viatura activa da mesma empresa; reagendar até `DELIVERED`; estados `UNSCHEDULED`→`SCHEDULED`→`DELIVERED`/`NOT_DELIVERED` (§3.1) | `DeliveryScheduleRulesTest` (`en01_*`) · `MigrationPostgresDialectHarnessTest` · `PostgresSchemaParityHarnessTest` |
| EN-02 | "Não entregue" exige motivo; reagendar volta a `SCHEDULED`; agendar não mexe no stock (§3.1) | `DeliveryScheduleRulesTest` (`en02_*`) |
| EN-03 | Agenda do dia por camião e período, só entregas agendadas da empresa activa, filtros dia/camião (§3.1) | `DeliveryScheduleRulesTest` (`en03_*`) |
| EN-04 | Entregue exige nome de quem recebeu; documento, assinatura e foto opcionais e validados pelo `ImagePayloadValidator`; quem/quando registados; prova imutável; auditoria `DELIVERY_CONFIRM` (§3.2) | `DeliveryProofTest` (`en04_*`) |
| EN-05 | PDF da guia mostra nome, data/hora e assinatura quando existe (§3.2) | `DeliveryProofTest` (`en05_*`) |
| EN-06 | Devolução só em entrega `DELIVERED`; 0 ≤ devolvida ≤ linha; motivo obrigatório; nasce `PENDING_RECEIPT` sem mexer no stock; cancelar sem stock (§3.3) | `SiteReturnTest` (`en06_*`) |
| EN-07 | Receber no armazém: só gestão, movimento `RETURN` no armazém da guia com lote, `RECEIVED`, auditoria; receber duas vezes recusado (§3.3) | `SiteReturnTest` (`en07_*`) |
| EN-08 | Guia faturada: recepção cria nota de crédito pendente ao preço da fatura com `stockAlreadyReturned`; aprová-la não mexe no stock; notas normais continuam a devolver stock; falha das regras da nota → aviso no sino e recepção válida (§3.3) | `SiteReturnTest` (`en08_*`) · `MigrationPostgresDialectHarnessTest` |
| EN-09 | Guia por faturar: a fatura da guia leva entregue − devolvido recebido por linha; linhas a zero ficam de fora (§3.3) | `SiteReturnTest` (`en09_*`) |
| EN-10 | Endpoints de agenda, entrega, não entrega, devolução e recepção; perfis da §3.4; empresa activa; rotas classificadas (§3.4) | `DeliveryScheduleHttpIntegrationTest` (`en10_*`) · `ApiRolePolicyEndpointCoverageHarnessTest` |
| EN-UI-01 | Guias de remessa: acções Agendar, Registar entrega (nome obrigatório no campo, quadro de assinatura, foto), Não entregue (§3.5) | `DeliveryAgendaUiHarnessTest` (`enUi01_*`) · `DesktopThinContextTest` |
| EN-UI-02 | Acções Devolução na obra e Receber devolução (só gestão) (§3.5) | `DeliveryAgendaUiHarnessTest` (`enUi02_*`) |
| EN-UI-03 | Separador Agenda de Entregas: dia (amanhã por omissão), camião, períodos, estado com `StatusTone`, abrir a guia (§3.5) | `DeliveryAgendaUiHarnessTest` (`enUi03_*`) |

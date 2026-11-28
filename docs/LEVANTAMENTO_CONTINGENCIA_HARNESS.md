# Harness — Talão de levantamento sem internet

> Cenários que provam [LEVANTAMENTO_CONTINGENCIA_SPEC.md](LEVANTAMENTO_CONTINGENCIA_SPEC.md).
> Um ID por regra verificável; o goal só marca um critério quando o teste da linha está verde.

**Última actualização:** 2026-11-28

| ID | Garantia | Teste |
|---|---|---|
| LC2-01 | Desktop guarda contas abertas e saldo por linha por empresa, gravação atómica, data da última actualização; lê-os sem servidor (§3.1) | `PickupOfflineCacheTest` (`lc201_*`) |
| LC2-02 | Provisório só em falha de rede; limite = saldo guardado − provisórios por sincronizar; conta fechada/cancelada recusada; prazo vencido só gestão; chave `CONT-LV-…` única (§3.2) | `PickupContingencyQueueTest` (`lc202_*`) |
| LC2-03 | Fila local durável e atómica, sobrevive a reinício; estados `PENDING_SYNC`/`SYNCED`/`REVIEW_NEEDED` (§3.2, §3.3) | `PickupContingencyQueueTest` (`lc203_*`) |
| LC2-04 | Talão provisório impresso no desktop com título, chave, artigos, quem levanta e aviso; só caracteres imprimíveis (§3.2) | `PickupContingencyQueueTest` (`lc204_*`) · `PrintableTextHarnessTest` |
| LC2-05 | Servidor: `requestKey` + `offlineIssuedAt`/`offlineIssuedBy` emite LV normal e guarda a chave e a hora real; repetir a chave devolve o mesmo talão (§3.3) | `PickupContingencySyncTest` (`lc205_*`) · `MigrationPostgresDialectHarnessTest` · `PostgresSchemaParityHarnessTest` |
| LC2-06 | Servidor: recusa de negócio grava incidente `REVIEW_NEEDED` com o talão e o motivo, avisa ADMIN/MANAGER no sino, não emite LV; repetir a chave não duplica o incidente (§3.3) | `PickupContingencySyncTest` (`lc206_*`) |
| LC2-07 | Sincronização FIFO a cada 45 s e "Sincronizar agora"; aceite → `SYNCED` com número LV; recusa → `REVIEW_NEEDED`; rede → fica na fila (§3.3) | `PickupContingencyQueueTest` (`lc207_*`) |
| LC2-08 | Resolver incidente: aceitar (só com saldo), cobrar a diferença (fatura do mesmo cliente, divide pelas duas contas), anular com motivo; só ADMIN/MANAGER; resolver duas vezes recusado; auditoria (§3.4) | `PickupContingencyIncidentTest` (`lc208_*`) |
| LC2-09 | Endpoints de sincronização e incidentes; perfis da §3.5; empresa activa; rotas classificadas (§3.5) | `PickupContingencyHttpIntegrationTest` (`lc209_*`) · `ApiRolePolicyEndpointCoverageHarnessTest` |
| LC2-UI-01 | Material por Levantar sem ligação: aviso com data dos saldos, emitir provisório com limite no campo, impressão, indicador "Sem ligação (N)" com lista e "Sincronizar agora" (§3.1–§3.3) | `PickupContingencyUiHarnessTest` (`lc2Ui01_*`) · `DesktopThinContextTest` |
| LC2-UI-02 | Ecrã de incidentes para a gestão: lista "Para rever", detalhe, aceitar / cobrar a diferença (escolher fatura) / anular com motivo (§3.4) | `PickupContingencyUiHarnessTest` (`lc2Ui02_*`) |

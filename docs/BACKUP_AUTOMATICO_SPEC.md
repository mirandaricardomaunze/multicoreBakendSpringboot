# Backup físico automático (agendado) + retenção + alerta

**Última actualização:** 2026-07-10
**Estado:** feito.

## Objectivo

A cópia de segurança física (pg_dump) existia só **manual** (botão "Backup Físico (BD)"). Uma loja não
se lembra de a fazer todos os dias — o maior risco de perda de dados. Passa a haver **backup físico
automático diário**, com **retenção** (apaga cópias antigas) e **alerta** quando falha.

## Comportamento

- **Agendado:** `@Scheduled(cron = "${backup.schedule.cron:0 0 23 * * *}")` — por omissão **23:00 diário**.
- **Estratégia por base:** PostgreSQL gera `.dump` físico restaurável; no ambiente H2 de
  desenvolvimento, a execução interactiva “Backup automático agora” usa o backup lógico JSON da
  empresa activa, evitando tentar executar `pg_dump` numa base incompatível.
  Só actua quando `backup.schedule.enabled=true` (perfis **desktop/prod**, PostgreSQL). Em dev/backend
  H2 fica desligado (o backup físico só suporta PostgreSQL).
- **Contexto de sistema:** corre sem utilizador autenticado, por isso chama
  `DatabaseBackupService.runPhysicalBackup()` (núcleo **sem** guarda de permissão), extraído de
  `executePhysicalBackup()` (que mantém a guarda ADMIN para o uso manual).
- **Retenção:** apaga ficheiros `*.dump` na pasta de backups com idade **> `backup.retention-days`**
  (default **30**). `retention-days ≤ 0` ⇒ retém tudo.
- **Alerta / registo:** cada execução regista auditoria (`BACKUP_AUTO_OK` / `BACKUP_AUTO_FAILED`,
  utilizador `SYSTEM`) + log (`INFO`/`ERROR`) + **estado da última execução** (`getLastRun()`) para a UI.
  Uma falha **não interrompe a app** — é registada e alertada.
- **UI (Config → Cópias de Segurança):** banner de estado ("Backup automático: ACTIVO — última: …
  ✓ OK / ✗ FALHOU", verde/vermelho) + botão **"Backup Automático Agora"** (ADMIN) para correr já e ver
  o resultado.

## Configuração (`application-desktop.properties` / `application-prod.properties`)

```properties
backup.schedule.enabled=true
backup.schedule.cron=0 0 23 * * *
backup.retention-days=30
backup.dir=backups          # já existente
backup.pg-bin-dir=          # já existente (binários pg_dump fora do PATH)
```

## Peças

- **`SchedulingConfig`** (`architecture/config`) — `@EnableScheduling`.
- **`DatabaseBackupService.runPhysicalBackup()`** — núcleo sem permissão (novo); `executePhysicalBackup()`
  = guarda ADMIN + núcleo.
- **`ScheduledBackupService`** — `@Scheduled` + `runAndRecord()` (backup + retenção + registo, não lança)
  + `applyRetention()` + `isExpiredBackup(...)` (pura, testável) + `getLastRun()`/`isEnabled()`.
- **`ConfigPanel`** — banner de estado + botão "Backup Automático Agora" (ADMIN), `refreshAutoBackupStatus()`.

## Notas / limites

- O `getLastRun()` é **em memória** (por sessão da app) — para o histórico persistente há a **auditoria**
  (`BACKUP_AUTO_*`, visível e filtrável no Log de Auditoria) e os próprios ficheiros na pasta.
- Requer `pg_dump` acessível (PATH ou `backup.pg-bin-dir`) — igual ao backup manual.
- O passo de **restaurar** continua manual e destrutivo (`DatabaseBackupService.restorePhysicalBackup`),
  fora do âmbito do automático.

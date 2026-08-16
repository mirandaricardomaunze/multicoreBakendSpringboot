# Harness — Backup físico automático (agendado)

> Cenários para [BACKUP_AUTOMATICO_SPEC.md](BACKUP_AUTOMATICO_SPEC.md).
> BA-01..06 automáticos (`ScheduledBackupServiceTest` — lógica pura de retenção); BA-50..55 manuais.

## Automático (`ScheduledBackupServiceTest` — `isExpiredBackup`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| BA-01 | `.dump` com 31 dias, retenção 30. | Expirado ⇒ elegível para apagar. |
| BA-02 | `.dump` com 10 dias, retenção 30. | Dentro do prazo ⇒ mantém. |
| BA-03 | Ficheiros não-`.dump` (`.txt`, `.json`). | Nunca elegíveis (não são backup físico). |
| BA-04 | Retenção 0 / negativa. | Retém tudo (nunca apaga). |
| BA-05 | Nome nulo. | Não rebenta; não elegível. |
| BA-06 | Extensão `.DUMP` (maiúsculas). | Reconhecida (case-insensitive). |

## Manuais

| ID    | Passos | Esperado |
|-------|--------|----------|
| BA-50 | Arrancar o desktop (perfil desktop, `backup.schedule.enabled=true`). | Arranca sem erros; agendamento activo (sem exceções no log). |
| BA-51 | Config → Cópias de Segurança: ver o banner de estado. | "Backup automático: ACTIVO (diário)". |
| BA-52 | Como **ADMIN**, clicar "Backup Automático Agora". | Gera um `.dump` em `backups/`; banner fica **✓ OK** (verde) com data/hora; consola mostra `[OK] …`. |
| BA-52H | Backend local em H2, clicar "Backup Automático Agora". | Gera backup lógico `.json`; não tenta `pg_dump`; estado fica OK. |
| BA-53 | Como não-ADMIN, clicar o botão. | Recusado ("Apenas administradores…"). |
| BA-54 | Ter `.dump` antigos (> retenção) na pasta e correr o backup. | Os antigos são apagados; a mensagem indica quantos removidos. |
| BA-55 | Simular falha (ex.: `backup.pg-bin-dir` inválido) e correr. | Banner fica **✗ FALHOU** (vermelho); auditoria regista `BACKUP_AUTO_FAILED`; app não rebenta. |
| BA-56 | Deixar chegar à hora do cron (ou pôr cron para daqui a 1–2 min) com a app aberta. | O backup corre sozinho; auditoria `BACKUP_AUTO_OK`; novo `.dump` na pasta. |

## Verificação

- `mvn -o test -Dtest=ScheduledBackupServiceTest` → verde (BA-01..06).
- `mvn -o compile` limpo; arranque do desktop sem erros de bean/agendamento.

# Especificação de Decomposição: DatabaseBackupService (Fase 151)

## 1. Diagnóstico do Monólito
`DatabaseBackupService.java` possuía **232 linhas**, misturando:
- Orquestração de backup e restore físico (`executePhysicalBackup`, `runPhysicalBackup`, `restorePhysicalBackup`).
- Parsing de URL JDBC PostgreSQL (`parsePgConnection`) e montagem de argumentos `pg_dump` / `pg_restore`.
- Execução isolada de processo de sistema operativo com `PGPASSWORD` e timeout (`runProcess`).

## 2. Arquitectura Alvo
1. `DatabaseBackupService` (`<= 115` linhas): Fachada `@Service` que valida ficheiros/permissões, preserva `PgConnection` e helpers package-private retrocompatíveis.
2. `PostgresBackupCommandBuilder` (`<= 90` linhas): Componente `@Component` que faz parse do URL JDBC e monta comandos `pg_dump` e `pg_restore`.
3. `PostgresBackupProcessRunner` (`<= 55` linhas): Componente `@Component` responsável pela execução de processos nativos com `PGPASSWORD` e timeout.

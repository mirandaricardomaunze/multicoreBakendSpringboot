# Harness de Verificação: DatabaseBackupService (Fase 151)

## Critérios de Aceitação
1. `DatabaseBackupService.java` $\le 115$ linhas físicas.
2. `PostgresBackupCommandBuilder.java` $\le 90$ linhas físicas.
3. `PostgresBackupProcessRunner.java` $\le 55$ linhas físicas.
4. `parsePgConnection`, `buildDumpCommand`, `buildRestoreCommand` e `PgConnection` preservados para retrocompatibilidade com testes unitários existentes.
5. Cobertura validada por `DatabaseBackupServiceDecompositionHarnessTest`.

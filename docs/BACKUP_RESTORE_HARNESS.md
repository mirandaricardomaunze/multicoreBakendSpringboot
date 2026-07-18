# Backup & Restore — Harness de validação

> Cenários para provar o backup físico restaurável. Spec em
> [BACKUP_RESTORE_SPEC.md](BACKUP_RESTORE_SPEC.md). Os BR-0x/1x são **automáticos**
> (`DatabaseBackupServiceTest`); os BR-5x são **manuais** (exigem PostgreSQL + `pg_dump`/`pg_restore`).

**Última actualização:** 2026-06-30

---

## Automáticos (`mvn test`)

| Id | Cenário | Espera-se |
|----|---------|-----------|
| BR-01 | `parsePgConnection("jdbc:postgresql://localhost:5432/multicore")` | host=`localhost`, porta=`5432`, db=`multicore` |
| BR-02 | URL sem porta (`jdbc:postgresql://db/multicore`) | porta default `5432` |
| BR-03 | URL com query (`…/multicore?sslmode=require`) | db=`multicore` (query ignorada) |
| BR-04 | URL não-postgres (`jdbc:h2:mem:test`) | `BusinessRuleException` (físico só em PostgreSQL) |
| BR-05 | `buildDumpCommand` | `[pg_dump, -h, host, -p, porta, -U, user, -F, c, -f, ficheiro, db]` |
| BR-06 | `buildDumpCommand` com `pg-bin-dir` definido | binário prefixado com a pasta |
| BR-07 | `buildRestoreCommand` | inclui `--clean --if-exists --no-owner -d db ficheiro` |
| BR-08 | `executePhysicalBackup` com role `EMPLOYEE` | `BusinessRuleException` (ADMIN-only) |
| BR-09 | `restorePhysicalBackup` com role `MANAGER` | `BusinessRuleException` (ADMIN-only) |
| BR-10 | `restorePhysicalBackup(path, confirmOverwrite=false)` (ADMIN) | `BusinessRuleException` (exige confirmação) |
| BR-11 | `restorePhysicalBackup` com ficheiro inexistente (ADMIN, confirmado) | `BusinessRuleException` (ficheiro não encontrado) |
| BR-12 | `restorePhysicalBackup` com ficheiro `.txt` (ADMIN, confirmado) | `BusinessRuleException` (não é `.dump`) |

> Nota: a **execução real** do `pg_dump`/`pg_restore` não é testada em CI (precisa de ambiente).
> Os testes cobrem parsing, construção de comando e guardas — tudo determinístico e sem ambiente.

---

## Manuais — o ciclo completo (ambiente separado)

Pré-requisitos: PostgreSQL a correr, BD `multicore` + role `multicore`, `DB_PASSWORD` definida,
binários `pg_dump`/`pg_restore` no `PATH` (ou `backup.pg-bin-dir` configurado).

| Id | Passo | Espera-se |
|----|-------|-----------|
| BR-50 | Login como **ADMIN** → aba *Cópias de Segurança* → **Backup Físico (BD)** | Cria `backups/multicore_<db>_<ts>.dump`; consola mostra o caminho |
| BR-51 | Inspeccionar o `.dump` (`pg_restore --list ficheiro.dump`) | Lista as tabelas (`invoices`, `products`, `stock_movements`, …) |
| BR-52 | **Round-trip:** criar uma 2.ª BD limpa `multicore_restore`; `pg_restore -d multicore_restore ficheiro.dump` | Restaura sem erros de FK/constraint |
| BR-53 | Comparar contagens nas duas BDs (`SELECT count(*)` por tabela-chave) | Contagens **idênticas** entre origem e restaurada |
| BR-54 | Apontar o desktop à BD restaurada (`DB_URL=…/multicore_restore`) e entrar | Login OK, faturas/stock/movimentos visíveis e íntegros |

> ⚠️ **BR-52 deve correr numa BD/instância separada**, nunca por cima da BD de produção. O
> `restorePhysicalBackup` da app usa `--clean` e **apaga** o conteúdo do alvo — só usar em
> recuperação real, com confirmação explícita.

---

## Critério de aceitação

- Todos os BR-0x/1x verdes em `mvn test`.
- BR-50..BR-54 executados pelo menos uma vez num ambiente separado, com **BR-53 a confirmar
  contagens idênticas** — só então o backup é declarado *restaurável* (fecha o item pendente de
  `tasks/current.md`: "Restore de backup testado em ambiente separado").
</content>

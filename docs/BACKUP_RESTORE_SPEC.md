# Backup & Restore — Especificação

> Fonte de verdade sobre **como o sistema faz cópias de segurança restauráveis** e como se
> prova o ciclo completo *backup → restore*. Lê este ficheiro antes de mexer em
> `modules/backup/`. Harness operacional em [BACKUP_RESTORE_HARNESS.md](BACKUP_RESTORE_HARNESS.md).

**Última actualização:** 2026-06-30

---

## 1. Problema

O `BackupService.executeBackup()` existente grava um **dump JSON lógico** por empresa. É útil
para **auditoria e verificação**, mas **não é restaurável com fidelidade**:

- Guarda apenas um **subconjunto de campos** por entidade (ex.: `products` guarda
  `sku/reference/barcode/name/unitPrice`, mas **não** `taxRate`, `imageData`, categoria, IVA…).
- Achata relações (guarda `productName`/`warehouseName` em vez de reconstruir os FKs).
- Restaurar a partir dele violaria `NOT NULL`/FK (ex.: `Product.taxRate`) e perderia dados.

**Conclusão:** o JSON é um *snapshot lógico de verificação*, não um backup de recuperação de
desastres (DR). Um backup que nunca foi restaurado não é um backup. Falta o **caminho físico**.

---

## 2. Decisão

Duas camadas, com papéis distintos e **não confundíveis**:

| Camada | Mecanismo | Restaurável? | Para quê |
|--------|-----------|:------------:|----------|
| **Lógica (existente)** | `BackupService` → JSON por empresa | ❌ (lossy) | Auditoria, verificação rápida, portabilidade de leitura |
| **Física (nova)** | `DatabaseBackupService` → `pg_dump`/`pg_restore` | ✅ fidelidade total | **Recuperação de desastres real** |

O backup físico usa as ferramentas nativas do PostgreSQL (formato custom `-Fc`), que é o que o
desktop e a produção já correm (perfil `desktop`/`prod`, Flyway + `validate`). Isto alinha com a
nota já existente em `tasks/current.md`: *"restore é ao nível de BD em ambiente separado"*.

---

## 3. Âmbito (`DatabaseBackupService`)

### 3.1 `executePhysicalBackup()` → caminho do ficheiro
- **Permissão:** `ADMIN` (`PermissionGuard.requireAdmin`).
- Lê a ligação de `spring.datasource.url/username/password`.
- **Recusa** se o URL não for `jdbc:postgresql://…` (ex.: H2 do backend puro de dev) com
  `BusinessRuleException` — o dump físico só faz sentido em PostgreSQL.
- Corre `pg_dump -F c` para `backups/multicore_<db>_<timestamp>.dump`.
- Password passada por **variável de ambiente `PGPASSWORD`** do subprocesso (nunca na linha de
  comando, que é visível noutros processos).
- Devolve o caminho absoluto do `.dump`.

### 3.2 `restorePhysicalBackup(path, confirmOverwrite)` → resumo
- **Permissão:** `ADMIN`.
- **Destrutivo:** substitui o conteúdo da BD. Exige `confirmOverwrite == true`; caso contrário
  lança `BusinessRuleException` (guarda contra restore acidental).
- Valida que o ficheiro existe, é ficheiro e termina em `.dump`.
- Corre `pg_restore --clean --if-exists --no-owner -d <db> <file>`.

### 3.3 Configuração
- `backup.pg-bin-dir` (default vazio) — pasta dos binários `pg_dump`/`pg_restore` quando não estão
  no `PATH` (ex.: `C:\Program Files\PostgreSQL\16\bin`).
- `backup.dir` (default `backups`) — pasta de destino, partilhada com o backup lógico.

### 3.4 Backup físico ≠ multi-tenant
O `pg_dump` é **da base de dados inteira** (todas as empresas), porque a recuperação de desastres
é da instância, não de um tenant. O backup **lógico** continua a ser por empresa. Esta diferença é
intencional e está documentada para não se confundir granularidades.

---

## 4. Regras / invariantes

1. **Nada de credenciais na linha de comando.** Password só via `PGPASSWORD` no ambiente do
   subprocesso.
2. **Restore é destrutivo e explícito.** Sem `confirmOverwrite` não corre.
3. **Falha do subprocesso = `BusinessRuleException`** com o `stderr` do `pg_dump`/`pg_restore`
   (mensagem accionável), nunca um erro engolido.
4. **Timeout** no subprocesso (default 10 min) para não pendurar a app.
5. **Camadas separadas:** o backup físico não toca no `BackupService` lógico nem nos seus testes.

---

## 5. Limites conhecidos (honestos)

- O ciclo *backup → wipe → restore → diff* **não é automatizável em CI** (precisa de PostgreSQL +
  binários `pg_dump`/`pg_restore` instalados). É um item de **harness manual** (BR-50..BR-54).
- Os testes automáticos cobrem o que é determinístico e sem ambiente: **parsing do JDBC URL**,
  **construção dos comandos**, **guardas de permissão** e **validação de ficheiro**. A execução real
  do subprocesso é coberta pelo harness manual.
- Tornar o JSON lógico *full-fidelity* e restaurá-lo continua a ser possível no futuro, mas é
  trabalho à parte e de maior risco; **não** é o caminho de DR recomendado.

---

## 6. Mapa de ficheiros

| Quero… | Ficheiro |
|--------|----------|
| Backup físico restaurável | `modules/backup/service/DatabaseBackupService.java` |
| Resultado do backup físico | `modules/backup/dto/PhysicalBackupResultDTO.java` |
| Backup/verificação lógica (JSON) | `modules/backup/service/BackupService.java` |
| Botão de backup físico | `gui/ConfigPanel.java` (aba Cópias de Segurança) |
| Cenários de validação | [BACKUP_RESTORE_HARNESS.md](BACKUP_RESTORE_HARNESS.md) |
</content>
</invoke>

# Paridade do schema PostgreSQL — Harness de validação

> Cenários que provam que as migrações produzem o schema exigido pelas entidades. Spec em
> [POSTGRES_SCHEMA_PARITY_SPEC.md](POSTGRES_SCHEMA_PARITY_SPEC.md).

**Última actualização:** 2026-10-31

## Como correr

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-backup-restore.ps1 -PgBinDir "C:/Program Files/PostgreSQL/18/bin"
```

O guião cria um cluster temporário em loopback, não toca na instância habitual e também corre o
ciclo backup/restauro. Na CI, o job `postgres-parity` executa o mesmo guião.

## Cenários

| Id | Cenário | Espera-se | Onde corre |
|----|---------|-----------|------------|
| PG-01 | Migração com `AUTO_INCREMENT`, `DATETIME`, `CLOB`, `BLOB`, `TINYINT` ou `LONGTEXT` | Reprova com ficheiro e linha | `mvn test` |
| PG-02 | Coluna de entidade sem migração | Reprova e nomeia a coluna | Guião e CI |
| PG-03 | Coluna com família de tipo diferente | Reprova e mostra os dois tipos | Guião e CI |
| PG-04 | `NOT NULL` na migração, anulável na entidade, fora das excepções | Reprova e nomeia a coluna | Guião e CI |
| PG-05 | CHECK que rejeita um valor do enum Java | Reprova e lista os valores | Guião e CI |
| PG-06 | Backend arranca em PostgreSQL novo com Flyway e `validate` | Contexto activo | Guião e CI |
| PG-07 | Backup, restauro, comparação integral e login no backend restaurado | Dados iguais; login 200 | Guião e CI |

## Evidência de 2026-10-31 (PostgreSQL 18)

| Prova | Resultado |
|-------|-----------|
| Antes da correcção | Falhou no V66: `syntax error at or near "AUTO_INCREMENT"` |
| Depois da correcção | 14 testes aprovados; 122 conjuntos comparados idênticos; login 200 |
| Regressão: `V105` sem a coluna `email` | Reprovado: `missing column [email] in table [app_users]` |
| Regressão: `V105` sem `updated_at` e sem `MPESA`/`EMOLA` | Reprovado com PG-04 e PG-05, nomeando as colunas e os valores |

Evidência local em `data/restore-validation/23e9d6d644ab4d53bf7fb54f67251fcb/evidence.txt`.

## Por validar

| Item | Porquê |
|------|--------|
| Primeira execução do job `postgres-parity` | Só corre no GitHub após push. No Linux, o guião usa pela primeira vez o `pg_config` do runner. |
| Protecção de branch a exigir o job | É uma definição do repositório, não do workflow. Ver README, "CI e gate de merge". |

## Critério de aceitação

- PG-01 verde em `mvn test`.
- PG-02 a PG-07 verdes no guião e no job `postgres-parity`.

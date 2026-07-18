# Harness — Desktop em PostgreSQL real

> Validação da [spec](BD_POSTGRES_DESKTOP_SPEC.md). Critério técnico: app arranca contra PostgreSQL,
> Flyway aplica `V1..V17` e Hibernate `validate` passa; dados persistem entre reinícios.

**Última actualização:** 2026-06-28

## Pré-requisitos

- PostgreSQL local a correr (porta 5432); BD `multicore` + role `multicore` criadas.
- Variável de ambiente `DB_PASSWORD` definida com a password da role.

| ID    | Passos                                                                                  | Esperado                                                                                  |
|-------|-----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| DB-01 | Arrancar o desktop (`java -cp … com.phcpro.desktop.DesktopApplication`)                  | Log mostra `jdbc:postgresql://…/multicore`; **não** aparece `jdbc:h2:mem`.                 |
| DB-02 | Observar o arranque                                                                      | Flyway: `Successfully applied 17 migrations … now at version v17`; sem erro de validação.  |
| DB-03 | Hibernate validate                                                                       | `Started DesktopApplication`; **sem** `Schema-validation: missing column …`.              |
| DB-04 | Criar um registo (ex.: cadastrar um produto) e **fechar** a app                          | Operação grava sem erro.                                                                   |
| DB-05 | Reabrir a app e procurar o registo                                                       | O registo **continua lá** (persistência confirmada — fim do H2 em memória).               |
| DB-06 | Recriar schema do zero (`DROP SCHEMA public CASCADE; CREATE SCHEMA …`) e arrancar        | Flyway reconstrói `V1..V17` numa BD limpa; app arranca igual.                              |

## Verificação técnica

```
# schema reconstruído por Flyway, validado por Hibernate:
#   Successfully applied 17 migrations to schema "public", now at version v17
#   Started DesktopApplication in … seconds

# coluna antes em falta agora presente:
psql -U multicore -d multicore -tAc \
  "SELECT count(*) FROM information_schema.columns \
   WHERE table_name='stock_transfers' AND column_name='approved_at';"   # -> 1
```

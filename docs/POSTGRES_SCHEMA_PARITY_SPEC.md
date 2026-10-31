# Paridade do schema PostgreSQL — Especificação

> Fonte de verdade sobre **como se garante que as migrações produzem o schema que as entidades
> exigem**. Harness em [POSTGRES_SCHEMA_PARITY_HARNESS.md](POSTGRES_SCHEMA_PARITY_HARNESS.md).

**Última actualização:** 2026-10-31

---

## 1. Problema

O desenvolvimento e o `mvn test` correm em H2 com `ddl-auto=update` e Flyway desligado. Produção
corre em PostgreSQL com Flyway e `ddl-auto=validate`. As migrações nunca eram executadas pela suite,
por isso divergiram das entidades sem nenhum teste reprovar.

Em 2026-10-31, a repetição de `scripts/verify-backup-restore.ps1` revelou o seguinte, com
2.504 testes verdes em H2:

| Defeito | Efeito em PostgreSQL |
|---------|----------------------|
| `AUTO_INCREMENT`/`DATETIME`/`CLOB` em V66, V76 e V89 | Instalação nova parava no V66 |
| V89 alterava `orders` (a tabela é `customer_orders`) | Migração falhava |
| V96 actualizava `quality_equipments`, tabela sem migração | Migração falhava |
| 2 tabelas de qualidade e 22 colunas sem migração | Backend não arrancava com `validate` |
| 9 colunas `updated_at NOT NULL` | Inserts falhavam: `BaseEntity` só preenche o campo em `@PreUpdate` |
| CHECK de `payment_entries.method` | Rejeitava `MPESA`, `EMOLA` e `STORE_CREDIT` |
| CHECK de `stock_movements.movement_type` | Rejeitava `WASTE` |
| CHECK de `till_movements.movement_type` | Rejeitava `REFUND` |
| `@Lob` em `AiDocumentVersion` | `pdf_data` esperado como `oid`, mas a coluna é `bytea` |

## 2. Decisão

1. **Corrigir V66, V76, V89 e V96 no próprio ficheiro.** É uma excepção à regra de nunca editar
   migrações aplicadas: estas falhavam em PostgreSQL, logo nenhuma base PostgreSQL as tem registadas
   como aplicadas, e o Flyway não tem checksum a comparar. O H2 não usa Flyway.
2. **Corrigir o resto em `V105__postgres_schema_parity.sql`**, a migração normal para o que já
   podia existir numa base.
3. **A entidade é a referência** para colunas e valores de enum. Colunas só presentes na migração
   (`updated_by`) ficam, pois são inofensivas.
4. **Alargamentos de tamanho e precisão ficam fora.** Divergências como `numeric(5,4)` em
   `tax_rate` são intencionais: as taxas são fracções (`0.16`).

## 3. Regras / invariantes

1. Nenhuma migração usa sintaxe fora do PostgreSQL (PG-01, em todo o `mvn test`).
2. Toda a coluna de entidade existe nas migrações com a mesma família de tipo (PG-02, PG-03).
3. Uma coluna anulável na entidade não pode ser `NOT NULL` na migração, salvo a excepção do §4 (PG-04).
4. Um CHECK de migração aceita todos os valores do enum Java (PG-05). **Acrescentar um valor a um
   enum exige verificar o CHECK correspondente.**
5. Campos `byte[]` e `String` longos não usam `@Lob`: em PostgreSQL usam `bytea` e `text`.

## 4. Excepções a PG-04

`store_vouchers.created_by` e `subscriptions.monthly_price` mantêm `NOT NULL` porque os serviços os
preenchem sempre (`PosReturnAndVoucherService`, `SubscriptionLifecycleService`,
`SubscriptionRenewalService`). Novas excepções exigem a mesma justificação.

## 5. Limites conhecidos

- A paridade compara schema, não comportamento. Carga e concorrência em PostgreSQL continuam por
  validar.
- Constraints únicas só presentes nas migrações são mais rígidas do que o H2 de desenvolvimento e
  ficam fora da comparação.
- `V105` adiciona `UNIQUE (stock_movement_id)`: numa base com duplicados, a migração falha e os
  duplicados têm de ser analisados antes.

## 6. Mapa de ficheiros

| Quero… | Ficheiro |
|--------|----------|
| Correcção de schema | `backend/src/main/resources/db/migration/V105__postgres_schema_parity.sql` |
| Guarda de dialecto | `MigrationPostgresDialectHarnessTest` |
| Comparação com PostgreSQL real | `PostgresSchemaParityHarnessTest` |
| Execução isolada | `scripts/verify-backup-restore.ps1` |
| Execução na CI | `.github/workflows/build.yml`, job `postgres-parity` |

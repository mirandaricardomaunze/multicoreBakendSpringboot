# Spec — Desktop em PostgreSQL real (dados persistentes)

> O cliente desktop deixa de usar H2 em memória e passa a usar **PostgreSQL local persistente**,
> com o schema gerido por **Flyway** e validado pelo Hibernate — igual ao modelo de produção.

**Última actualização:** 2026-06-28

## Problema

O perfil `desktop` herdava de [application.properties](../src/main/resources/application.properties) o
datasource `jdbc:h2:mem:phcprodb`. Resultado: **todos os dados desapareciam ao fechar a app**, tornando
impossível usar o ERP no dia-a-dia da loja.

## Decisão

- **Datasource PostgreSQL no perfil desktop.**
  [application-desktop.properties](../src/main/resources/application-desktop.properties) passa a definir
  `jdbc:postgresql://localhost:5432/multicore`, role dedicada `multicore` (owner da BD), driver PG e
  dialeto PG.
- **Credenciais fora do git.** A password vem de `${DB_PASSWORD}` (variável de ambiente persistente da
  máquina, definida com `setx`). O ficheiro de propriedades só tem placeholders; `DB_URL`/`DB_USER` têm
  defaults locais. Cópia de recurso da password local em `~/.multicore_db_pw.txt`.
- **Flyway dono do schema + Hibernate `validate`.** Mesmo modelo de
  [application-prod.properties](../src/main/resources/application-prod.properties): Flyway aplica
  `V1..V17`; Hibernate apenas valida. Sem `ddl-auto=update` no desktop.
- **`V17__sync_schema_with_entities.sql`.** Durante o desenvolvimento o desktop corria com
  `ddl-auto=update`, pelo que algumas mudanças de entidade nunca tiveram migração. A `V17` fecha esse
  desvio (derivada do diff que o Hibernate `update` gerava contra `V1..V16`):
  - colunas em falta: `stock_transfers.approved_at`, `approved_by`, `rejection_reason`;
  - precisão `numeric(38,2)` em `purchase_orders` e `purchase_order_lines`;
  - 7 restrições `UNIQUE` declaradas nas entidades (employees ×2, payslips, payroll_bonuses,
    product_batches, stocks, tax_rates).

## Não-objetivos

- Não alterar o **backend puro de dev** (`mvn spring-boot:run` / `application.properties`), que continua
  em H2 + `ddl-auto=update` para arranque rápido sem servidor.
- Não gerir a instalação/serviço do PostgreSQL (assume-se um servidor local a correr).
- Não migrar dados antigos de H2 (era em memória — não havia dados persistentes a migrar).

## Notas técnicas

- A role e a BD criam-se uma vez (como superuser):
  `CREATE ROLE multicore LOGIN PASSWORD '…'; CREATE DATABASE multicore OWNER multicore;`.
- Recriar o schema do zero (sem dados): `DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION
  multicore;` e arrancar a app — o Flyway reconstrói `V1..V17`.
- O caminho de geração de migrações novas mantém-se: alterar entidades → gerar diff (Hibernate `update`
  contra o schema Flyway numa BD temporária) → escrever `V18+` → voltar a `validate`.

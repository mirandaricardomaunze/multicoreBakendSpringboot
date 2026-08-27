# Migrações Flyway

Esta pasta contém as migrações SQL versionadas que o Flyway aplica em **produção** (perfil `prod`).

## Convenção de naming

```
V<NÚMERO>__<descrição_kebab>.sql        # migração normal (ex: V1__init.sql)
R__<nome>.sql                            # repetível (ex: R__views.sql)
```

## Regenerar a migração inicial (V1__init.sql)

O `V1__init.sql` baseline **já existe** (schema completo, gerado para o dialecto
PostgreSQL). Só é preciso regenerá-lo se o schema base mudar de forma que não
caiba numa migração incremental V<n> (o caminho normal para alterações é **criar
uma nova migração**, não reescrever a V1).

Para regenerar a partir das entidades JPA:

1. Em `application.properties`, **descomentar** as 4 linhas no fim do ficheiro
   (`hibernate.dialect=...PostgreSQLDialect` + `schema-generation.scripts.action=create`
   + `create-target` + `delimiter`) e pôr `spring.jpa.hibernate.ddl-auto=none`.
   Forçar o dialecto PostgreSQL é o que faz o Hibernate emitir `bigserial`,
   `timestamp(6)`, `bytea`, etc. em vez do DDL de H2.
2. Apagar o ficheiro `V1__init.sql`.
3. Correr `mvn spring-boot:run` — o script é escrito no arranque do JPA (a app
   pode falhar logo a seguir por não haver tabelas; o ficheiro já ficou gerado).
4. **Repor** `application.properties` (recomentar as 4 linhas, `ddl-auto=update`).
5. Rever o ficheiro e voltar a pôr o cabeçalho de proveniência.
6. Commit.

## Aplicar em produção

Com `SPRING_PROFILES_ACTIVE=prod` e as variáveis `DB_URL`/`DB_USER`/`DB_PASSWORD`
definidas, o Spring Boot corre o Flyway no arranque. `baseline-on-migrate=true`
permite aplicar pela primeira vez sobre uma base já existente.

## Regras

- **Nunca editar uma migração já aplicada em produção.** Criar uma nova.
- Não correr `ddl-auto=update` em prod — só Flyway mexe no schema.
- Cada migração deve ser **idempotente quando possível** (`CREATE TABLE IF NOT EXISTS`)
  para tolerar reaplicação manual em ambientes de teste.

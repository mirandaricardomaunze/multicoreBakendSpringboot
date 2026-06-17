# Base de Dados - Multicore ERP

Este documento define regras de persistencia, migrations e integridade. Complementa `src/main/resources/db/migration/README.md`.

## Stack

- Dev local: H2.
- Producao alvo: PostgreSQL.
- ORM: JPA/Hibernate.
- Migrations: SQL versionado em `src/main/resources/db/migration`.

## Regras de entidade

- Entidades vivem em `modules/<dominio>/model`.
- Entidades de negocio devem herdar `BaseEntity` quando fizer sentido.
- Tabelas e colunas em ingles, `snake_case`.
- Tabelas no plural: `products`, `stock_movements`, `payroll_tax_configs`.
- IDs tecnicos usam `Long`.
- Dinheiro e quantidades usam `BigDecimal` com `precision` e `scale`.
- Datas sem hora usam `LocalDate`; timestamps usam `LocalDateTime` ou `Instant` conforme padrao existente.

## Relacionamentos JPA

- `@ManyToOne` e `@OneToOne` devem usar `fetch = FetchType.LAZY`.
- `@OneToMany` deve ser LAZY por defeito.
- Usar `JOIN FETCH` em Repository quando a leitura precisa percorrer relacoes.
- Evitar cascades amplos em agregados grandes.
- Nao serializar Entity directamente em Controller.

## Tenant e empresa

- Dados operacionais devem ser filtrados pela empresa actual quando aplicavel.
- Usar `CurrentUserContext` e Services de seguranca existentes.
- Nunca confiar num `companyId` vindo do cliente sem validar acesso.
- Queries agregadas devem respeitar escopo de tenant.

## Migrations

Formato:

```text
V<numero>__descricao_curta_em_ingles.sql
```

Regras:

- Nunca editar migration ja aplicada em ambiente partilhado.
- Criar nova migration para alteracoes de schema.
- Migrations destrutivas exigem decisao explicita do utilizador.
- Seeds fiscais ou legais devem ser idempotentes quando possivel.
- Indices devem acompanhar finders frequentes e chaves de busca.

## Integridade

Usar constraints na BD para proteger invariantes estruturais:

- `not null` para campos obrigatorios.
- `unique` para codigos, series e numeros quando aplicavel.
- FK para relacoes obrigatorias.
- Indices para tenant, data, status, numero e referencias de documento.

Mas erros de utilizador devem ser tratados antes no Service com `BusinessRuleException`, para mensagem clara.

## Checklist para nova tabela

- [ ] Nome da tabela esta em ingles e plural.
- [ ] Ha entidade no modulo dono.
- [ ] Campos monetarios usam `BigDecimal`.
- [ ] Campos obrigatorios tem validacao no DTO e constraint na BD.
- [ ] Relacoes usam LAZY.
- [ ] Tenant/empresa foi considerado.
- [ ] Indices foram adicionados para consultas importantes.
- [ ] Migration nova foi criada.
- [ ] Service cobre regras antes de persistir.

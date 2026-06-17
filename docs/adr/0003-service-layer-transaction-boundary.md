# ADR 0003 - Service como fronteira transaccional

## Estado

Aceite.

## Contexto

Operacoes do ERP frequentemente afectam varias areas ao mesmo tempo: factura, stock, caixa, fiscal e auditoria. Se cada camada controlar a sua transaccao, o sistema pode ficar parcialmente actualizado.

## Decisao

Services sao a fronteira transaccional:

- Escritas usam `@Transactional`.
- Leituras agregadas usam `@Transactional(readOnly = true)`.
- Controllers, Repositories e paineis Swing nao controlam transaccoes.

## Consequencias

Positivas:

- Casos de uso ficam atomicos.
- Rollback em erro de negocio e previsivel.
- UI e API reutilizam a mesma regra.

Custos:

- Services precisam ser bem desenhados.
- Chamar metodos internos com transaccao exige cuidado com proxy Spring.

## Guardrails

- Uma operacao de negocio deve ter um Service dono.
- Nao dividir checkout/factura/stock/tesouraria em chamadas UI independentes.
- `BusinessRuleException` deve abortar a transaccao quando a regra falha.

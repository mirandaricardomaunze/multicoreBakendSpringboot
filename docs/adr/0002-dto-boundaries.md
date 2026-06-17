# ADR 0002 - DTOs obrigatorios nas fronteiras

## Estado

Aceite.

## Contexto

Entidades JPA carregam relacoes, detalhes internos e risco de serializacao acidental. A API precisa ser estavel para clientes HTTP e para o desktop.

## Decisao

Toda fronteira externa usa DTOs:

- Requests: records com Bean Validation.
- Responses: records sem dados sensiveis.
- Entities ficam dentro do modulo e nao saem do Service.

## Consequencias

Positivas:

- API mais estavel.
- Menor acoplamento ao schema.
- Mais seguranca contra exposicao acidental.
- Melhor compatibilidade com desktop HTTP.

Custos:

- Mais codigo de mapeamento.
- Necessidade de manter DTOs alinhados com casos de uso.

## Guardrails

- Controller nao recebe `@Entity`.
- Controller nao devolve `@Entity`.
- Mapper fica no Service ou helper dedicado.
- DTO nao contem regra de negocio.

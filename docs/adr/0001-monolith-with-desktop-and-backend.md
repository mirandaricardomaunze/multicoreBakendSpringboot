# ADR 0001 - Monolito modular com backend e desktop no mesmo codebase

## Estado

Aceite.

## Contexto

O Multicore ERP tem backend Spring Boot e cliente desktop Swing. Durante a migracao, ambos vivem no mesmo codebase para acelerar desenvolvimento e reutilizar Services.

## Decisao

Manter um monolito modular com dois entrypoints:

- Backend: `com.phcpro.MulticoreApplication`.
- Desktop: `com.phcpro.desktop.DesktopApplication`.

A regra de negocio fica nos Services dos modulos. O Swing pode chamar Services durante a fase actual, mas a meta e substituir por clients HTTP.

## Consequencias

Positivas:

- Menos duplicacao de regra.
- Desenvolvimento local simples.
- Migracao gradual para backend online.

Custos:

- Risco de UI conhecer detalhes demais.
- Disciplina forte de camadas e necessaria.
- O desktop deve ser escrito como futuro consumidor HTTP.

## Guardrails

- Regras nunca vivem no Swing.
- Controllers e clients HTTP devem chamar os mesmos casos de uso.
- Services nao devem conhecer componentes Swing.

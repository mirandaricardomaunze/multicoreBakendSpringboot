---
id: GOAL-002
title: "Módulos lêem produtos e categorias só pelo ProductLookup"
status: IN_PROGRESS
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["ProductLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 50 leituras e gravações directas de `ProductRepository`/`ProductCategoryRepository` fora do módulo comercial.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [x] MD-P01 — ProductLookup (com categorias) e comandos de gravação no Service de produto — `ProductLookupTest`
- [ ] MD-P02 — inventory sem Product/ProductCategory Repository — `ModuleDecouplingHarnessTest`
- [ ] MD-P03 — purchases, crm, architecture, promotions, pricing, pos, manufacturing, backup — `ModuleDecouplingHarnessTest`
- [ ] MD-P04 — restantes módulos — `ModuleDecouplingHarnessTest`
- [ ] MD-P05 — zero ligações a Product/ProductCategory Repository fora de comercial; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).
- 2026-11-11 · MD-P01 · `comercial.service.ProductLookup` (produtos e categorias; `require*` com "Produto não encontrado." / "Categoria não encontrada.", `find*` com Optional para manter mensagens já testadas) e `ProductCommandService` (`saveProduct`, `saveCategory`) para gravações alheias; `ProductLookupTest` verde. Nota: os testes correm a partir da raiz do reactor (DTOs no módulo `contracts`).

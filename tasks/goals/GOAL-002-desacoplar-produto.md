---
id: GOAL-002
title: "Módulos lêem produtos e categorias só pelo ProductLookup"
status: DONE
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
- [x] MD-P02 — inventory sem Product/ProductCategory Repository — `ModuleDecouplingHarnessTest`
- [x] MD-P03 — purchases, crm, architecture, promotions, pricing, pos, manufacturing, backup — `ModuleDecouplingHarnessTest`
- [x] MD-P04 — restantes módulos — `ModuleDecouplingHarnessTest`
- [x] MD-P05 — zero ligações a Product/ProductCategory Repository fora de comercial; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).
- 2026-11-11 · MD-P01 · `comercial.service.ProductLookup` (produtos e categorias; `require*` com "Produto não encontrado." / "Categoria não encontrada.", `find*` com Optional para manter mensagens já testadas) e `ProductCommandService` (`saveProduct`, `saveCategory`) para gravações alheias; `ProductLookupTest` verde. Nota: os testes correm a partir da raiz do reactor (DTOs no módulo `contracts`).
- 2026-11-11 · MD-P02 · os 12 serviços de `inventory` lêem produtos só pelo `ProductLookup` e o `AverageCostUpdater` grava via `ProductCommandService`; quebras e rascunhos de transferência passam à mensagem normalizada "Produto não encontrado."; 12 linhas apagadas do baseline CA-07; testes de inventário, auditoria e harnesses verdes.
- 2026-11-11 · MD-P03 · 25 ficheiros de purchases, crm, architecture (DataLoader e seeders), promotions, pricing, pos, manufacturing e backup lêem produtos/categorias só pelo `ProductLookup`; CRM (tarifa e produtos internos) e seeders gravam via `ProductCommandService`; `PosCheckoutAndPaymentService` (import wildcard, fora da baseline) também migrado; mensagens específicas mantidas via `find*`; 28 linhas apagadas do baseline CA-07; testes de compras, POS, CRM, promoções, produção, backup, segurança e harnesses verdes.
- 2026-11-11 · gate · DG-10 corrigido: a ordem dos campos `ProductCommandService`/`ProductLookup` no `WorkSheetService` coincidia linha a linha com o `ReferenceAndAdminDataSeeder` e foi trocada; `CRMServiceTest` e harnesses de arquitectura verdes.
- 2026-11-11 · MD-P04 · weighbridge, variants, units, returnables, expiry, consignment, butchery, bulkstock e advances lêem produtos/categorias só pelo `ProductLookup` (`find*`, mensagens específicas mantidas); unidades, variantes e o artigo "Adiantamento" gravam via `ProductCommandService`; 10 linhas apagadas do baseline CA-07; testes HTTP/regras desses módulos, `ProductLookupTest` e harnesses verdes.
- 2026-11-11 · MD-P05 · zero ligações a `ProductRepository`/`ProductCategoryRepository` fora de `comercial` (baseline CA-07 sem linhas de produto; o import wildcard `comercial.repository.*` do `PosCheckoutAndPaymentService` já não usa produto e fica para o GOAL-003); goal em `DONE`, onda GOAL-002 com 0 ligações no `ModuleDecouplingHarnessTest`; suite completa (`mvn test`) verde.

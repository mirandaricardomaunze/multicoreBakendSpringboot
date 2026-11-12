---
id: GOAL-003
title: "Documentos comerciais e clientes só por Lookups do comercial"
status: IN_PROGRESS
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["ClientLookupTest", "InvoiceLookupTest", "InvoiceCommandServiceTest", "OrderLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 79 leituras directas de facturas, clientes, encomendas, cotações, guias, recibos e notas fora do módulo comercial.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [x] MD-V01 — ClientLookup e leitores de ClientRepository migrados — `ClientLookupTest`
- [x] MD-V02 — InvoiceLookup e migração de inventory e printing — `InvoiceLookupTest`
- [x] MD-V03 — restantes leitores de InvoiceRepository — `ModuleDecouplingHarnessTest`
- [x] MD-V04 — OrderLookup (encomendas, cotações, guias) e leitores migrados — `OrderLookupTest`
- [ ] MD-V05 — recibos, notas de crédito e de débito migrados — `ModuleDecouplingHarnessTest`
- [ ] MD-V06 — zero ligações a comercial.repository fora de comercial; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).
- 2026-11-11 · MD-V01 · `ClientLookup` (`requireForCompany`/`findForCompany`, `require`/`find`, `listForCompany`; "Cliente não encontrado." normalizado) e `ClientCommandService.saveClient` para o seeder demo; os 15 leitores de fora (advances, backup, consignment, crm, pos — incluindo o `PosCheckoutAndPaymentService` do import wildcard —, pricing, projects, recurring, returnables, DataLoader/DemoEnterpriseDataSeeder) migrados, 15 linhas apagadas da baseline CA-07; `ClientLookupTest`, testes dos módulos e harnesses verdes.
- 2026-11-12 · MD-V02 · `InvoiceLookup` (`require`/`find` com "Fatura não encontrada.", `listCreatedBetween`); `printing` (Invoice/Receipt/GuideRemittancePrintService) e `inventory` (StockWasteService/StockWasteAnalyticsService) lêem facturas só pelo Lookup; 5 linhas apagadas da baseline CA-07; `InvoiceLookupTest`, testes de inventory/printing (81 classes), `AuditTrailCriticalEventsHarnessTest` e harnesses verdes.
- 2026-11-12 · MD-V03 · `InvoiceLookup` alargado (`listForCompany`, `listAll`, `listByStatus`, `findByContingencyReference`, `sumQuantitySoldByProductSince`) e novo `InvoiceCommandService` (`lockForCompany`, `save`) para POS, tesouraria e adiantamentos; os 25 leitores restantes (advances, audit, backup, consignment, financeira, fiscal, intelligence, manufacturing, movimentos, performance, pickup, pos — incluindo o `PosCheckoutAndPaymentService` do import wildcard —, projects, purchases, reports, returnables) migrados, mensagens específicas mantidas; 25 linhas apagadas da baseline CA-07. O wildcard `comercial.repository.*` do `PosCheckoutAndPaymentService` fica para o MD-V04 (só usa `QuotationRepository`).
- 2026-11-12 · MD-V04 · `OrderLookup` (encomendas `requireOrder`/`findOrder`/`findOrderForCompany`/`listOrdersForCompany`/`findOrderByInvoice`; cotações `listQuotationsForCompany`/`findQuotationForCompany`/`findQuotationWithLines`; guias `listDeliveryGuidesForCompany`/`findDeliveryGuideWithLines`; "Encomenda não encontrada." normalizado) e `OrderCommandService` (`saveOrder`, `markQuotationConvertedByPos` — a conversão da cotação no POS passou para o dono, a auditoria fica no POS); advances, customorders, fleet, movimentos, pos (incluindo o wildcard do `PosCheckoutAndPaymentService`), printing e projects migrados, mensagens específicas mantidas; 20 linhas apagadas da baseline CA-07; `OrderLookupTest` e 184 classes de teste (módulos tocados + architecture) verdes. Nota: compilar pelo reactor da raiz (`-pl backend -am`), porque os DTOs vivem no módulo `contracts`.

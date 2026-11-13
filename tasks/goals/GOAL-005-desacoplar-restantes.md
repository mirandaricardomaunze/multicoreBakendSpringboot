---
id: GOAL-005
title: "Nenhum módulo lê o Repository de outro"
status: IN_PROGRESS
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["AppUserLookupTest", "EmployeeLookupTest", "SupplierLookupTest", "PosLookupsTest", "TreasuryAccountLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 95 ligações restantes (users, hr, purchases, pos, financeira, fleet, fiscal, approvals, ...) e esvaziar a baseline do CA-07.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [x] MD-R01 — AppUserLookup e leitores migrados — `AppUserLookupTest`
- [x] MD-R02 — EmployeeLookup e leitores migrados — `EmployeeLookupTest`
- [x] MD-R03 — SupplierLookup e leitores migrados — `SupplierLookupTest`
- [x] MD-R04 — pos: Lookup das sessões/vendas e leitores migrados — `ModuleDecouplingHarnessTest`
- [ ] MD-R05 — TreasuryAccountLookup e leitores migrados — `TreasuryAccountLookupTest`
- [ ] MD-R06 — fleet, fiscal, approvals, manufacturing, assets, recurring, performance, audit, quality, crm — `ModuleDecouplingHarnessTest`
- [ ] MD-R07 — zero imports de Repository alheio; baseline do CA-07 vazia; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).
- 2026-11-13 · MD-R01 · `AppUserLookup` (`require`/`find`, `requireByUsername`/`findByUsername`, `listAll`, `listForCompany`, `countActiveByCompanyAndRole`, `countAccessesForCompany`; "Utilizador não encontrado.") e `AppUserCommandService` (`saveUser`/`saveUsers`); 12 ficheiros migrados (seeders, `TenantAccessService`, backup, monitoring, platform) e 15 linhas apagadas da baseline CA-07; `AppUserLookupTest`, harnesses e testes das classes alteradas verdes.
- 2026-11-13 · MD-R02 · `EmployeeLookup` (`require`/`find`: "Colaborador não encontrado."; `requireForCompany`/`findForCompany`: "Colaborador não encontrado nesta empresa."; `listForCompany`, `listByStatusForCompany`; recibos `findPayslip` e contratos `requireContractForCompany`: "Contrato não encontrado.", por serem do agregado mais próximo) e `EmployeeCommandService` (`saveEmployee`, `savePayslip`); 11 ficheiros migrados (seeders, performance, quality, printing) e 14 linhas apagadas da baseline CA-07; `EmployeeLookupTest`, harnesses e testes das classes alteradas verdes. Nota: compilar a partir da raiz (`mvn -f pom.xml -pl backend -am`), porque os DTOs vivem no módulo `contracts`.
- 2026-11-13 · MD-R03 · `SupplierLookup` (`require`/`find`: "Fornecedor não encontrado."; `listForCompany`, `listActiveForCompany`; documentos de compra do agregado mais próximo: `listPurchasesForCompany`, `listAllPurchases`, `listPurchaseOrdersForCompany`), `BusinessUnitLookup.findForCompany` (Optional: comercial mantém "Linha de negócio (restrita) não encontrada.") e `SupplierCommandService.saveSupplier` (seeder); 13 ficheiros migrados (seeders, backup, comercial, cash-flow, IVA, saúde ocupacional, assistente IA) e 16 linhas apagadas da baseline CA-07; asserção textual do `AiManagementAssistantHarnessTest` passa a exigir `supplierLookup.listPurchaseOrdersForCompany(companyId)`; `SupplierLookupTest`, harnesses e testes de purchases/hr/backup/fiscal/financeira/intelligence/comercial verdes.
- 2026-11-13 · MD-R03 (gate) · diff-guard DG-07: nas linhas alteradas de `ComercialInvoiceLifecycleService` e `ComercialProductCatalogService` o `@Autowired(required = false) BusinessUnitLookup` passou a `@Nullable` (`org.springframework.lang`), mesma injecção opcional por construtor; harnesses e testes `Comercial*` verdes.
- 2026-11-13 · MD-R04 · `TillSessionLookup` (`listForCompany`, `listMovementsForCompany`) e `PosPaymentLookup` (`listForInvoice`, `listPaidForCompanyBetween`; vales e pagamentos móveis do agregado mais próximo: `requireVoucher` "Vale de compras não encontrado.", `findVoucherByCode`, `findMobilePaymentForCompany`); 8 ficheiros migrados (backup, printing, reports, subscription) e 12 linhas apagadas da baseline CA-07; `PosLookupsTest`, harnesses e testes de backup/printing/reports/subscription verdes.

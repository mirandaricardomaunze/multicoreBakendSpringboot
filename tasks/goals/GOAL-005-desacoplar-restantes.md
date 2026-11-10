---
id: GOAL-005
title: "Nenhum módulo lê o Repository de outro"
status: TODO
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["AppUserLookupTest", "EmployeeLookupTest", "SupplierLookupTest", "TreasuryAccountLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 95 ligações restantes (users, hr, purchases, pos, financeira, fleet, fiscal, approvals, ...) e esvaziar a baseline do CA-07.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [ ] MD-R01 — AppUserLookup e leitores migrados — `AppUserLookupTest`
- [ ] MD-R02 — EmployeeLookup e leitores migrados — `EmployeeLookupTest`
- [ ] MD-R03 — SupplierLookup e leitores migrados — `SupplierLookupTest`
- [ ] MD-R04 — pos: Lookup das sessões/vendas e leitores migrados — `ModuleDecouplingHarnessTest`
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

---
id: GOAL-001
title: "Módulos lêem a empresa só pelo CompanyLookup"
status: TODO
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["CompanyLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 106 leituras directas de `CompanyRepository` noutros módulos, a maior fonte de acoplamento do sistema.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [ ] MD-C01 — CompanyLookup com require/reference/findAll/findByTaxId/lockForUpdate e mensagens PT-MZ — `CompanyLookupTest`
- [ ] MD-C02 — hr sem CompanyRepository — `ModuleDecouplingHarnessTest`
- [ ] MD-C03 — inventory e comercial sem CompanyRepository — `ModuleDecouplingHarnessTest`
- [ ] MD-C04 — purchases, platform, fleet, users — `ModuleDecouplingHarnessTest`
- [ ] MD-C05 — manufacturing, crm, assets, architecture, support, subscription, recurring, promotions, pos, performance, intelligence, backup, approvals — `ModuleDecouplingHarnessTest`
- [ ] MD-C06 — restantes módulos com 1 ficheiro — `ModuleDecouplingHarnessTest`
- [ ] MD-C07 — zero ligações a company.repository fora de company; suite completa verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).

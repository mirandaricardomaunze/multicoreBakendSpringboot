---
id: GOAL-004
title: "Stock e armazéns só por Lookups e pelo ledger do inventário"
status: TODO
priority: HIGH
spec: docs/MODULE_DECOUPLING_SPEC.md
harness: docs/MODULE_DECOUPLING_HARNESS.md
verify: ["WarehouseLookupTest", "StockLookupTest", "ModuleDecouplingHarnessTest", "CleanArchitectureHarnessTest"]
blockedBy: []
---

# Objectivo

Retirar as 65 leituras e escritas directas de armazéns, stock, quebras, transferências, lotes e movimentos fora do inventário. Toda a escrita de stock passa pelo serviço de movimentos.

Refactorização pura (SPEC §3): nenhum endpoint, resultado, código HTTP ou regra muda.
Cada critério de migração apaga as suas linhas de
`backend/src/test/resources/architecture/cross-module-repository-baseline.txt` (só apagar, nunca acrescentar).

## Critérios de aceitação

- [ ] MD-I01 — WarehouseLookup e leitores migrados — `WarehouseLookupTest`
- [ ] MD-I02 — StockLookup só de leitura; escrita alheia pelo serviço de movimentos — `StockLookupTest`
- [ ] MD-I03 — leitores de StockRepository migrados sem mudar saldos — `ModuleDecouplingHarnessTest`
- [ ] MD-I04 — quebras, transferências, lotes e movimentos migrados — `ModuleDecouplingHarnessTest`
- [ ] MD-I05 — zero ligações a inventory.repository fora de inventory; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).

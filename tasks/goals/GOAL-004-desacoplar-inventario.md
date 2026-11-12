---
id: GOAL-004
title: "Stock e armazéns só por Lookups e pelo ledger do inventário"
status: IN_PROGRESS
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

- [x] MD-I01 — WarehouseLookup e leitores migrados — `WarehouseLookupTest`
- [x] MD-I02 — StockLookup só de leitura; escrita alheia pelo serviço de movimentos — `StockLookupTest`
- [x] MD-I03 — leitores de StockRepository migrados sem mudar saldos — `ModuleDecouplingHarnessTest`
- [ ] MD-I04 — quebras, transferências, lotes e movimentos migrados — `ModuleDecouplingHarnessTest`
- [ ] MD-I05 — zero ligações a inventory.repository fora de inventory; suite verde — `ModuleDecouplingHarnessTest`

## Fora de âmbito

- Usos do Repository dentro do próprio módulo dono.
- Trocar relações JPA por IDs; outras ondas de desacoplamento.

## Perguntas em aberto

(Vazio.)

## Registo de progresso

- 2026-11-10 · goal criado a partir da auditoria do acoplamento (CA-07).
- 2026-11-12 · MD-I01 · `WarehouseLookup` (`require`/`find`, `requireForCompany`/`findForCompany`, `listForCompany`; "Armazém não encontrado." / "Armazém não encontrado nesta empresa.") e `WarehouseCommandService.saveWarehouse` para o seeder; 29 consumidores migrados e 29 linhas apagadas da baseline CA-07.
- 2026-11-12 · MD-I02 · `StockLookup` só de leitura (`find`, `quantityOf` com zero sem registo, `listForCompany`/`listForWarehouse`/`listForProduct`); a única escrita alheia (stock inicial do `DemoEnterpriseDataSeeder`) passa por `StockMovementService.registerMovement` (ENTRY, como SYSTEM na empresa do armazém), com o mesmo saldo e agora com movimento e lote; `StockLookupTest` guarda que nada fora do inventário faz `save`/`delete` num `StockRepository`; 2 linhas apagadas da baseline CA-07 (`DataLoader`, `DemoEnterpriseDataSeeder`).
- 2026-11-12 · MD-I03 · 18 leitores de `StockRepository` (backup, bulkstock, butchery, consignment, intelligence, manufacturing, printing, productionplan, purchases, reports, returnables) passam a `StockLookup` (`quantityOf`, `listForCompany`, `listForWarehouse`); a verificação de faltas da produção usa `StockLookup.findForReservation` (lock pessimista, `Propagation.MANDATORY`); `StockLookupTest` guarda que nada fora do inventário importa o `StockRepository`; 18 linhas apagadas da baseline CA-07.

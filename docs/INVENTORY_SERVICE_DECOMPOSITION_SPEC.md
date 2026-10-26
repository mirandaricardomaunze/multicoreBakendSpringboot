# Especificação Técnica — Decomposição Modular de InventoryService (Fase 97)

## 1. Contexto e Diagnóstico

`InventoryService.java` acumulava mais de 735 linhas e 9 dependências diretas, agrupando sob uma mesma classe responsabilidades distintas:
1. Gestão administrativa de armazéns (criação, edição, desativação, tipo de armazém, controlo de permissão de vendas no POS e trancamento de contagem cega);
2. Consultas analíticas de saldos, catálogo multi-armazém para o POS, alertas de ruptura de stock e alertas de validade FEFO;
3. Execução de movimentos de entrada, saída FEFO e ajustes físicos de inventário com idempotência SHA-256 e bloqueio pessimista `findForReservation`.

## 2. Princípios Arquiteturais e Diretrizes Aplicadas

- **SOLID - Princípio da Responsabilidade Única (SRP):** Decomposição em 3 serviços especializados com fronteiras de responsabilidade estritas.
- **Injeção de Dependências por Construtor:** Sub-serviços anotados com `@Service` e injetados via construtor com campos `final`.
- **Preservação de Concorrência e Bloqueios Pessimistas:** Cumprimento rigoroso do `MultiUserRobustnessHarnessTest` com `findForReservation`.
- **Preservação de Paginação no Servidor:** Cumprimento rigoroso do `StockMovementPaginationHarnessTest` com `findPageByCompanyId`.
- **Limites Físicos de Linhas:**
  - `InventoryService.java`: $\le 320$ linhas (orquestrador enxuto de ~311 linhas).
  - `WarehouseService.java`: $\le 215$ linhas (~205 linhas).
  - `StockQueryService.java`: $\le 205$ linhas (~193 linhas).
  - `StockMovementService.java`: $\le 380$ linhas (~369 linhas).

## 3. Estrutura Modular Decomposta

```
backend/src/main/java/mz/multicore/erp/modules/inventory/service/
├── InventoryService.java         (Orquestrador enxuto, ~272 linhas)
├── WarehouseService.java         (Ciclo de vida de armazéns e venda, ~183 linhas)
├── StockQueryService.java        (Saldos, catálogos POS e alertas FEFO, ~172 linhas)
└── StockMovementService.java     (Movimentos, ajustes e idempotência, ~318 linhas)
```

## 4. Colaboradores Especializados

### 4.1 WarehouseService
- Gestão de armazéns: `getWarehousesByCompany`, `getSalesWarehousesByCompany`, `getAllWarehousesByCompany`, `resolveSalesWarehouses`.
- CRUD e permissões: `createWarehouse`, `updateWarehouse`, `setWarehouseActive`.
- Bloqueio de contagem cega: `isStockCountLocked`, `setStockCountLocked`.

### 4.2 StockQueryService
- Alertas e catálogo para POS: `findOutOfStockProducts`, `getInStockProductIdsForSale`, `getStockQuantitiesForSale`.
- Consultas de saldos: `getStocksByCompany`, `getStocksByWarehouse`, `getPhysicalQuantity`, `lockAndGetPhysicalQuantity`.
- Alertas de lotes e validades: `findBatchesByCompany`, `findExpiringBatchesByCompany`, `findBatchesByWarehouse`, `findNextFEFO`, `findExpiringBatches`.

### 4.3 StockMovementService
- Movimentos com lote e validade: `registerMovement` (entradas, saídas FEFO com migração preguiçosa `ensureLegacyBatchIfNeeded`).
- Idempotência SHA-256: `registerMovement(RegisterMovementRequest)`, `adjustStock(CreateStockAdjustmentRequest)`.
- Bloqueio pessimista de concorrência: `adjustStock(Product, Warehouse, BigDecimal)` via `stockRepository.findForReservation`.
- Histórico paginado: `findMovementsPageByCompany` via `stockMovementRepository.findPageByCompanyId`.

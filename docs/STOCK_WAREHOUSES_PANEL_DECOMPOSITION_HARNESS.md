# Harness de Testes & Validação — Decomposição do StockWarehousesPanel

**ID:** `HARNESS-STOCK-WAREHOUSES-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-STOCK-WAREHOUSES-PANEL-DECOMPOSITION-001`  
**Alvo:** `mz.multicore.erp.gui.StockWarehousesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivos do Harness

1. **Garantia de Tetos de Linhas:**
   - `StockWarehousesPanel.java` $\le 195$ linhas.
   - `WarehouseFormDialog.java` $\le 95$ linhas.
   - `StockWarehouseViewCoordinator.java` $\le 75$ linhas.

2. **Invariantes Arquiteturais e de Layout:**
   - Conformidade com `StockUnifiedHarnessTest` (STOCK-03): presença de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(owner.warehousesTable)`, `TableQuickPeekController.install(owner.warehousesTable`.
   - Conformidade com `StockWarehouseKpiHarnessTest` (H-WMS-01): presença de `KpiCard.createGrid(4)` com "Armazéns Activos", "Capacidade Instalada", "Valor em Armazém" e "Rede de Distribuição".
   - Conformidade com `TableActionPlacementHarnessTest`: presença de `UIHelper.tableCardTop(`.
   - Manutenção de 0 chamadas a `JOptionPane`.

3. **Verificação de Regressão e Contexto:**
   - Suíte unitária `StockWarehousesPanelDecompositionHarnessTest`.
   - `StockUnifiedHarnessTest`, `StockWarehouseKpiHarnessTest`, `TableActionPlacementHarnessTest`, `ProfessionalFeedbackHarnessTest`.
   - `DesktopInitializationTest` e `DesktopThinContextTest`.
   - Reactor Maven `MultiModuleArchitectureHarnessTest` em `backend`.

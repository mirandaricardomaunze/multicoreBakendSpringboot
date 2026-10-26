# Harness de Testes & Validação — Decomposição do PurchaseBusinessUnitsPanel

**ID:** `HARNESS-PURCHASE-BUSINESS-UNITS-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-PURCHASE-BUSINESS-UNITS-PANEL-DECOMPOSITION-001`  
**Alvo:** `mz.multicore.erp.gui.PurchaseBusinessUnitsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivos do Harness

1. **Garantia de Tetos de Linhas:**
   - `PurchaseBusinessUnitsPanel.java` $\le 180$ linhas.
   - `BusinessUnitFormDialog.java` $\le 95$ linhas.
   - `PurchaseBusinessUnitsOperationsCoordinator.java` $\le 65$ linhas.

2. **Invariantes Arquiteturais e de Layout:**
   - Presença obrigatória de `KpiCard.createGrid(3, 8, 8)`.
   - Presença obrigatória de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`.
   - Presença de `UIHelper.tableCardTop("Linhas de Negócio / Distribuição"`.
   - Manutenção de 0 chamadas a `JOptionPane`.

3. **Verificação de Regressão e Contexto:**
   - Suíte unitária `PurchaseBusinessUnitsPanelDecompositionHarnessTest`.
   - `DesktopInitializationTest` e `DesktopThinContextTest`.
   - Reactor Maven `MultiModuleArchitectureHarnessTest` em `backend`.

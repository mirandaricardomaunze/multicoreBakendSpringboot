# Plano de Testes & Validação: Decomposição de StockBatchesPanel

**Identificador:** `HARNESS-STOCK-BATCHES-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/STOCK_BATCHES_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração de `StockBatchesPanel` reduz a sua extensão para $\le 165$ linhas sem quebrar nenhum dos contratos literais esperados por `StockPanelDecompositionHarnessTest`, `TableActionPlacementHarnessTest`, `DesktopInitializationTest` e `DesktopThinContextTest`.

---

## 2. Casos de Teste do Harness (`StockBatchesPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `StockBatchesPanel.java` possui $\le 165$ linhas.
2. **`testCollaboratorLineCount`:**
   - Valida que `StockBatchesViewCoordinator.java` possui $\le 140$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `StockBatchesPanel.java` contém:
     - `UIHelper.tableCardTop("Lotes & Validades"`
     - `new ModernPanel(16)`
     - `TableFilter.bar(`
     - `UIHelper.ensureHeadersFit(owner.batchesTable)`
     - `ClientTablePagination.install(owner.batchesTable)`
     - `TableQuickPeekController.install(owner.batchesTable`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
5. **`testPublicApiPreserved`:**
   - Valida a existência do construtor package-private `StockBatchesPanel(StockPanel owner)`.
   - Valida a existência dos métodos públicos `buildPanel()` e `refresh()`.

# HARNESS-STOCK-CATEGORIES-PANEL-DECOMPOSITION-001: Matriz de Testes e Validação do Painel de Categorias

## 1. Critérios de Aceitação & Catracas
1. **Linhas de Código:**
   - `StockCategoriesPanel.java` $\le 130$ linhas (anterior: 250 linhas).
   - `StockCategoryFormDialog.java` $\le 95$ linhas.
   - `StockCategoriesViewCoordinator.java` $\le 125$ linhas.
2. **Zero `JOptionPane`:**
   - 0 ocorrências de `JOptionPane` em `StockCategoriesPanel.java`, `StockCategoryFormDialog.java` e `StockCategoriesViewCoordinator.java`.
3. **Contratos Canónicos Preservados:**
   - `StockCategoriesPanel(StockPanel owner)`.
   - `UIHelper.tableCardTop("Categorias de Produto"`.
   - `UIHelper.ensureHeadersFit(owner.categoriesTable)`.
   - `TableQuickPeekController.install(owner.categoriesTable`.
   - `TableFilter.bar(`.
   - Ausência estrita de `new JPanel(new GridBagLayout())` em filtros.

## 2. Testes de Regressão e Homologação
- `StockCategoriesPanelDecompositionHarnessTest` (novo conjunto unitário).
- `StockUnifiedHarnessTest` (`STOCK-03`).
- `TableActionPlacementHarnessTest`.
- `DesktopInitializationTest`.
- `DesktopThinContextTest`.
- `MultiModuleArchitectureHarnessTest`.

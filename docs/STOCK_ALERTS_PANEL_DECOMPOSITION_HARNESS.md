# HARNESS-STOCK-ALERTS-PANEL-DECOMPOSITION-001: Matriz de Testes e Validação do Painel de Alertas de Stock

## 1. Critérios de Aceitação & Catracas
1. **Linhas de Código:**
   - `StockAlertsPanel.java` $\le 165$ linhas (anterior: 253 linhas).
   - `StockAlertsViewCoordinator.java` $\le 120$ linhas.
2. **Zero `JOptionPane`:**
   - 0 ocorrências de `JOptionPane` em `StockAlertsPanel.java` e `StockAlertsViewCoordinator.java`.
3. **Contratos Canónicos Preservados:**
   - `StockAlertsPanel(StockPanel owner)`.
   - `UIHelper.tableCardTop("Lotes Expirados e a Expirar"`.
   - `UIHelper.tableCardTop("Produtos Esgotados"`.
   - `UIHelper.ensureHeadersFit(owner.alertsExpTable)`.
   - `UIHelper.ensureHeadersFit(owner.alertsOutTable)`.
   - `TableFilter.bar(`.
   - Ausência estrita de `new JPanel(new GridBagLayout())` em filtros.

## 2. Testes de Regressão e Homologação
- `StockAlertsPanelDecompositionHarnessTest` (novo conjunto unitário).
- `StockUnifiedHarnessTest` (`STOCK-02`).
- `TableActionPlacementHarnessTest`.
- `DesktopInitializationTest`.
- `DesktopThinContextTest`.
- `MultiModuleArchitectureHarnessTest`.

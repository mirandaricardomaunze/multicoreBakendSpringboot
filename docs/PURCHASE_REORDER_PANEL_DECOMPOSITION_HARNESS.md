# HARNESS-PURCHASE-REORDER-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Reposição Inteligente

## 1. Identificação do Harness
- **ID**: `HARNESS-PURCHASE-REORDER-PANEL-DECOMPOSITION-001`
- **Domínio**: Compras & Reposição Inteligente (`desktop/src/main/java/mz/multicore/erp/gui/PurchaseReorderPanel.java`)
- **Fase de Execução**: Fase 29

## 2. Casos de Teste do Harness

| ID | Teste | Alvo | Condição de Sucesso |
| :--- | :--- | :--- | :--- |
| **PRP-01** | `testLineCount` | `PurchaseReorderPanel.java` | Linhas totais $\le 190$ |
| **PRP-02** | `testCollaboratorsLineCount` | Novos colaboradores | `PurchaseReorderOperationsCoordinator.java` $\le 85$ linhas, `PurchaseReorderDataCoordinator.java` $\le 115$ linhas |
| **PRP-03** | `testCanonicalContractsPreserved` | `PurchaseReorderPanel.java` | Presença de `KpiCard.createGrid(4)`, `KpiCard.createInteractiveCard(`, `new ModernPanel(16)`, `UIHelper.tableCardTop(`, `TableQuickPeekController.install(reorderTable`, `filterUrgency(`, `UIHelper.ensureHeadersFit(reorderTable)` |
| **PRP-04** | `testNoLegacyDialogs` | Todos os ficheiros | Zero ocorrências de `JOptionPane` |

## 3. Matriz de Cobertura de Regressão
Para garantir zero regressão, devem ser validados os seguintes testes:
1. `PurchaseReorderPanelDecompositionHarnessTest`: (4/4)
2. `PurchaseReorderUnifiedHarnessTest`: (5/5)
3. `TableActionPlacementHarnessTest`: (6/6)
4. `DesktopInitializationTest`: (1/1)
5. `DesktopThinContextTest`: (2/2)
6. `MultiModuleArchitectureHarnessTest`: (6/6)

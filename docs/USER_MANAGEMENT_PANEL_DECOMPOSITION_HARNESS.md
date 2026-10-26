# HARNESS-USER-MANAGEMENT-PANEL-DECOMPOSITION-001: Matriz de Testes e Validação do Painel de Utilizadores

## 1. Critérios de Aceitação & Catracas
1. **Linhas de Código:**
   - `UserManagementPanel.java` $\le 150$ linhas (anterior: 260 linhas).
   - `UserOperationsCoordinator.java` $\le 130$ linhas.
2. **Zero `JOptionPane`:**
   - 0 chamadas a `JOptionPane` em `UserManagementPanel.java`, `UserOperationsCoordinator.java`, `UserEditorDialog.java` e `ManagerPinDialog.java`.
3. **Contratos Canónicos Preservados:**
   - Construtor público `UserManagementPanel(UserApiClient)`.
   - `panel.getComponentCount() == 2`.
   - `UIHelper.tableCardTop("Gestão de Utilizadores"`.
   - `UIHelper.ensureHeadersFit(usersTable)`.
   - `TableQuickPeekController.install(usersTable`.
   - `KpiCard.createGrid(4)`.

## 2. Testes de Regressão e Homologação
- `UserManagementPanelDecompositionHarnessTest` (novo conjunto unitário).
- `UserManagementPanelHarnessTest` (`GUP-07`, `GUP-08`).
- `AdminApprovalsUnifiedHarnessTest` (`AA-01`).
- `TableActionPlacementHarnessTest`.
- `DesktopInitializationTest`.
- `DesktopThinContextTest`.
- `MultiModuleArchitectureHarnessTest`.

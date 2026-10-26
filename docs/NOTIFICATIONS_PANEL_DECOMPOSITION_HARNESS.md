# HARNESS-NOTIFICATIONS-PANEL-DECOMPOSITION-001: Matriz de Testes e Validação do Painel de Notificações

## 1. Critérios de Aceitação & Catracas
1. **Linhas de Código:**
   - `NotificationsPanel.java` $\le 150$ linhas (anterior: 239 linhas).
   - `NotificationsViewCoordinator.java` $\le 110$ linhas.
2. **Zero `JOptionPane`:**
   - 0 ocorrências ou imports de `JOptionPane` em `NotificationsPanel.java` e `NotificationsViewCoordinator.java`.
3. **Contratos Canónicos Preservados:**
   - `NotificationsPanel.rowFor(item, read)` funcional.
   - `NotificationsPanel.summaryText(total, unread)` funcional.
   - `UIHelper.tableCardTop("Notificações"`.
   - `UIHelper.ensureHeadersFit(table)`.
   - `TableQuickPeekController.install(table`.

## 2. Testes de Regressão e Homologação
- `NotificationsPanelDecompositionHarnessTest` (novo conjunto unitário).
- `NotificationsPanelTest`.
- `TableActionPlacementHarnessTest`.
- `DesktopInitializationTest`.
- `DesktopThinContextTest`.
- `MultiModuleArchitectureHarnessTest`.

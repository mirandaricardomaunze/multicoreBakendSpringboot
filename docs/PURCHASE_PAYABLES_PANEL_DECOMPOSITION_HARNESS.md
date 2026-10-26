# HARNESS-PURCHASE-PAYABLES-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Contas a Pagar

## 1. Casos de Teste do Harness

O harness automatizado em `PurchasePayablesPanelDecompositionHarnessTest.java` valida rigorosamente:

- **PPP-01:** `PurchasePayablesPanel.java` deve possuir $\le 115$ linhas de código.
- **PPP-02:** `SupplierPaymentDialog.java` deve possuir $\le 95$ linhas de código.
- **PPP-03:** `PurchasePayablesViewCoordinator.java` deve possuir $\le 110$ linhas de código.
- **PPP-04:** Erradicação absoluta de `JOptionPane` em `PurchasePayablesPanel.java`, `SupplierPaymentDialog.java` e `PurchasePayablesViewCoordinator.java`.
- **PPP-05:** Preservação estrita dos contratos de construtor `PurchasePayablesPanel(ComprasPanel)`, métodos `buildPanel()`, `refresh()` e invariantes de layout (`UIHelper.tableCardTop`, `TableFilter.bar`, `TableQuickPeekController.install(owner.payablesTable`).

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `PurchasePayablesPanelDecompositionHarnessTest` | Verificação estrita das catracas de linhas, desacoplamento e ausência de JOptionPane |
| `PurchasesUnifiedHarnessTest` | Verificação de layout canónico `PURCH-02` (sem cabeçalho duplicado, TableFilter.bar, Quick Peek) |
| `TableActionPlacementHarnessTest` | Verificação de `tableCardTop` canónico no painel |
| `ProfessionalFeedbackHarnessTest` | Confirmação de 0 chamadas a `JOptionPane` em todo o módulo compras |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Validação de arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

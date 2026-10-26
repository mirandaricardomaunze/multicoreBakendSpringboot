# HARNESS-PURCHASE-SUPPLIERS-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Fornecedores

## 1. Casos de Teste do Harness

O harness automatizado em `PurchaseSuppliersPanelDecompositionHarnessTest.java` valida:

- **PSP-01:** `PurchaseSuppliersPanel.java` deve possuir $\le 95$ linhas de código.
- **PSP-02:** `SupplierFormDialog.java` deve possuir $\le 75$ linhas de código.
- **PSP-03:** `PurchaseSuppliersViewCoordinator.java` deve possuir $\le 80$ linhas de código.
- **PSP-04:** Erradicação absoluta de `JOptionPane` em `PurchaseSuppliersPanel.java`, `SupplierFormDialog.java` e `PurchaseSuppliersViewCoordinator.java`.
- **PSP-05:** Preservação estrita dos contratos de construtor `PurchaseSuppliersPanel(ComprasPanel)`, método `buildPanel()` e invariantes de layout (`UIHelper.tableCardTop`, `TableFilter.bar`, `TableQuickPeekController.install(owner.suppliersTable`, `UIHelper.ensureHeadersFit(owner.suppliersTable)`).

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `PurchaseSuppliersPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `PurchasesUnifiedHarnessTest` | Verificação de layout canónico `PURCH-03` (`TableFilter.bar`, Quick Peek, `ensureHeadersFit`) |
| `TableActionPlacementHarnessTest` | Verificação de `tableCardTop` canónico no painel |
| `ProfessionalFeedbackHarnessTest` | Confirmação de 0 chamadas a `JOptionPane` em todo o módulo compras |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Validação de arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

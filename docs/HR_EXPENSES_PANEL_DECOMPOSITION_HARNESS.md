# HARNESS-HR-EXPENSES-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Notas de Despesas

## 1. Casos de Teste do Harness

O harness automatizado em `HRExpensesPanelDecompositionHarnessTest.java` valida:

- **HEP-01:** `HRExpensesPanel.java` deve possuir $\le 75$ linhas de código.
- **HEP-02:** `HrExpenseClaimDialog.java` deve possuir $\le 75$ linhas de código.
- **HEP-03:** `HrExpensesViewCoordinator.java` deve possuir $\le 65$ linhas de código.
- **HEP-04:** Erradicação absoluta de `JOptionPane` em `HRExpensesPanel.java`, `HrExpenseClaimDialog.java` e `HrExpensesViewCoordinator.java`.
- **HEP-05:** Preservação estrita dos contratos de construtor `HRExpensesPanel(HRPanel)`, métodos `buildPanel()`, `refresh()` e invariantes de layout (`UIHelper.tableCardTop`, `TableFilter.bar`, `TableQuickPeekController.install(table`, `UIHelper.ensureHeadersFit(table)`).

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `HRExpensesPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `HRUnifiedHarnessTest` | Verificação de layout canónico `HR-03` (`ensureHeadersFit` e `TableQuickPeekController`) |
| `TableActionPlacementHarnessTest` | Verificação de `tableCardTop` canónico no painel |
| `ProfessionalFeedbackHarnessTest` | Confirmação de 0 chamadas a `JOptionPane` em todo o módulo de RH |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Validação de arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

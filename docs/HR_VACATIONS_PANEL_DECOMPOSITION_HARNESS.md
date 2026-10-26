# HARNESS-HR-VACATIONS-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Pedidos de Férias

## 1. Casos de Teste do Harness

O harness automatizado em `HRVacationsPanelDecompositionHarnessTest.java` valida:

- **HVP-01:** `HRVacationsPanel.java` deve possuir $\le 110$ linhas de código.
- **HVP-02:** `HrVacationFormDialog.java` deve possuir $\le 90$ linhas de código.
- **HVP-03:** `HrVacationsViewCoordinator.java` deve possuir $\le 90$ linhas de código.
- **HVP-04:** Erradicação absoluta de `JOptionPane` em `HRVacationsPanel.java`, `HrVacationFormDialog.java` e `HrVacationsViewCoordinator.java`.
- **HVP-05:** Preservação estrita dos contratos de construtor `HRVacationsPanel(HRPanel)`, métodos `buildPanel()`, `load()`, `openVacationAllowance()` e invariantes de layout (`UIHelper.tableCardTop`, `TableQuickPeekController.install(table`, `UIHelper.ensureHeadersFit(table)`).

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `HRVacationsPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `HRUnifiedHarnessTest` | Verificação de layout canónico `HR-02` (`ensureHeadersFit` e `TableQuickPeekController`) |
| `HRSubsidiesUiHarnessTest` | Verificação da descoberta da ação "Subsídio de Férias" e chamada a `openVacationAllowance()` |
| `HrCrmUiErgonomicsHarnessTest` | Validação de cabeçalho `tableCardTop` com no máximo 3 botões e menu "Decisão & Subsídio" |
| `TableActionPlacementHarnessTest` | Verificação de `tableCardTop` canónico no painel |
| `ProfessionalFeedbackHarnessTest` | Confirmação de 0 chamadas a `JOptionPane` em todo o módulo de RH |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Validação de arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

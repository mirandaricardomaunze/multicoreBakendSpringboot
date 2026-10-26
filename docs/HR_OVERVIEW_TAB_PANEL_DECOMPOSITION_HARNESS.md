# HARNESS-HR-OVERVIEW-TAB-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição da Visão Geral de RH

## 1. Casos de Teste do Harness

O harness automatizado em `HROverviewTabPanelDecompositionHarnessTest.java` valida:

- **HOT-01:** `HROverviewTabPanel.java` deve possuir $\le 110$ linhas de código (redução de ~45%).
- **HOT-02:** `HROverviewMetricsCoordinator.java` deve possuir $\le 115$ linhas de código.
- **HOT-03:** Erradicação absoluta de `JOptionPane` em `HROverviewTabPanel.java` e `HROverviewMetricsCoordinator.java`.
- **HOT-04:** Preservação estrita dos contratos de construtor package-private `HROverviewTabPanel(HRPanel)` e métodos `buildPanel()`, `refresh()`.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `HROverviewTabPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `HrPanelDecompositionHarnessTest` | Verificação do módulo principal de RH |
| `HRUnifiedHarnessTest` | Testes integrados de RH |
| `ProfessionalFeedbackHarnessTest` | Ausência de modais bloqueantes legados |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

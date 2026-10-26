# HARNESS-CONFIG-SUPPORT-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Suporte

## 1. Casos de Teste do Harness

O harness automatizado em `ConfigSupportPanelDecompositionHarnessTest.java` valida:

- **CSP-01:** `ConfigSupportPanel.java` deve possuir $\le 105$ linhas de código (redução de ~45%).
- **CSP-02:** `ConfigSupportTicketDialog.java` deve possuir $\le 90$ linhas de código.
- **CSP-03:** Erradicação absoluta de `JOptionPane` em `ConfigSupportPanel.java` e `ConfigSupportTicketDialog.java`.
- **CSP-04:** Preservação estrita dos contratos de construtor package-private `ConfigSupportPanel(ConfigPanel)` e métodos públicos `buildPanel()`, `refresh()`.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `ConfigSupportPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `ConfigPanelDecompositionHarnessTest` | Verificação regressiva do módulo de configuração geral |
| `ProfessionalFeedbackHarnessTest` | Ausência de modais bloqueantes legados |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

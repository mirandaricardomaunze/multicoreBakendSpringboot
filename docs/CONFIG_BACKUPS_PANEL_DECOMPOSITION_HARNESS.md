# HARNESS-CONFIG-BACKUPS-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Backups

## 1. Casos de Teste do Harness

O harness automatizado em `ConfigBackupsPanelDecompositionHarnessTest.java` valida:

- **CBP-01:** `ConfigBackupsPanel.java` deve possuir $\le 115$ linhas de código (redução de ~50%).
- **CBP-02:** `ConfigBackupOperations.java` deve possuir $\le 125$ linhas de código.
- **CBP-03:** Erradicação absoluta de `JOptionPane` em `ConfigBackupsPanel.java` e `ConfigBackupOperations.java`.
- **CBP-04:** Preservação estrita dos contratos de construtor package-private `ConfigBackupsPanel(ConfigPanel)` e métodos `buildPanel()`, `refresh()`, `refreshAutoBackupStatus()`, `loadBackupFilesList()`.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `ConfigBackupsPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `ConfigPanelDecompositionHarnessTest` | Verificação regressiva do módulo de configuração geral |
| `ProfessionalFeedbackHarnessTest` | Ausência de modais bloqueantes legados |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

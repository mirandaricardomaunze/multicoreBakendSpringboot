# HARNESS-CONFIG-DOCUMENT-COLUMNS-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Colunas de Documentos

## 1. Casos de Teste do Harness

O harness automatizado em `ConfigDocumentColumnsPanelDecompositionHarnessTest.java` valida:

- **CDC-01:** `ConfigDocumentColumnsPanel.java` deve possuir $\le 125$ linhas de código.
- **CDC-02:** `ConfigDocumentColumnsOperations.java` deve possuir $\le 105$ linhas de código.
- **CDC-03:** Erradicação absoluta de `JOptionPane` em `ConfigDocumentColumnsPanel.java` e `ConfigDocumentColumnsOperations.java`.
- **CDC-04:** Preservação estrita dos contratos de construtor package-private `ConfigDocumentColumnsPanel(ConfigPanel)` e métodos `buildPanel()`, `refresh()`, `loadDocumentColumns()`.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `ConfigDocumentColumnsPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `ConfigPanelDecompositionHarnessTest` | Verificação regressiva do módulo de configuração geral |
| `ProfessionalFeedbackHarnessTest` | Ausência de modais bloqueantes legados |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

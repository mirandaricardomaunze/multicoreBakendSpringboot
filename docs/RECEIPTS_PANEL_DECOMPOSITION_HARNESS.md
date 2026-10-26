# HARNESS-RECEIPTS-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição de Recibos

## 1. Casos de Teste do Harness

O harness automatizado em `ReceiptsPanelDecompositionHarnessTest.java` valida:

- **RCP-01:** `ReceiptsPanel.java` deve possuir $\le 115$ linhas de código (redução > 50%).
- **RCP-02:** `ReceiptPaymentDialog.java` deve possuir $\le 85$ linhas de código e `ReceiptsViewCoordinator.java` deve possuir $\le 85$ linhas de código.
- **RCP-03:** Erradicação absoluta de `JOptionPane` em `ReceiptsPanel.java`, `ReceiptPaymentDialog.java` e `ReceiptsViewCoordinator.java`.
- **RCP-04:** Preservação estrita dos contratos de construtor `ReceiptsPanel(ComercialApiClient, Runnable)` e métodos públicos `openPayment(...)`, `refresh()`, além dos padrões de layout `UIHelper.tableCardTop(...)`, `TableFilter.bar(...)`, `UIHelper.ensureHeadersFit(...)` e `TableQuickPeekController.install(...)`.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `ReceiptsPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `CommercialOperacionalUnifiedHarnessTest` | Validação de conformidade operacional canónica (COMM-OP-02) |
| `TableActionPlacementHarnessTest` | Validação de regras ergonómicas de posicionamento de botões |
| `ComercialPanelDecompositionHarnessTest` | Integração do painel de recibos no cockpit comercial |
| `ProfessionalFeedbackHarnessTest` | Erradicação global de modais bloqueantes |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

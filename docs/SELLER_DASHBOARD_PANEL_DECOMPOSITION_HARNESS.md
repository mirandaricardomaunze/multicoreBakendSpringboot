# HARNESS-SELLER-DASHBOARD-PANEL-DECOMPOSITION-001: Critérios de Verificação da Decomposição do Painel do Vendedor

## 1. Casos de Teste do Harness

O harness automatizado em `SellerDashboardPanelDecompositionHarnessTest.java` valida:

- **SDP-01:** `SellerDashboardPanel.java` deve possuir $\le 110$ linhas de código.
- **SDP-02:** `SellerDashboardViewCoordinator.java` deve possuir $\le 95$ linhas de código.
- **SDP-03:** Erradicação absoluta de `JOptionPane` em `SellerDashboardPanel.java` e `SellerDashboardViewCoordinator.java`.
- **SDP-04:** Preservação estrita dos contratos de construtor `SellerDashboardPanel(ComercialApiClient, POSApiClient, Consumer<String>)`, métodos públicos `updateWelcomeMessage(String)`, `refreshData()`, 4 KPIs canónicos de 96px e 6 botões de ação rápida.

---

## 2. Matriz de Compatibilidade Regressiva

| Test Suite | Propósito |
| :--- | :--- |
| `SellerDashboardPanelDecompositionHarnessTest` | Verificação estrita de catracas de linhas, desacoplamento e ausência de JOptionPane |
| `SellerExperienceHarnessTest` | Verificação da experiência UX do vendedor (UXV-01 a UXV-05) |
| `SellerLeanStartupHarnessTest` | Validação de arranque enxuto para utilizador com perfil SELLER |
| `DesktopInitializationTest` & `DesktopThinContextTest` | Validação de arranque íntegro da aplicação desktop |
| `MultiModuleArchitectureHarnessTest` | Preservação das fronteiras modulares `contracts`, `backend`, `desktop` |

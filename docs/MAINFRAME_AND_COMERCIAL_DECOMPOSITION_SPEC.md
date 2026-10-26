# MainFrame and ComercialPanel Decomposition Specification

## 1. Contexto e Diagnóstico
Para manter a modularidade, navegabilidade e baixa complexidade cognitiva, a arquitetura do Multicore ERP impõe um teto estrito de no máximo 1.000 linhas por painel/classe principal de interface gráfica (`MAX_PANEL_LINES = 1000`), verificado permanentemente pelos testes:
- `UiPanelDecompositionTest`
- `HighContrastThemeHarnessTest`
- `RecentItemsHistoryHarnessTest`
- `UiDensityZoomHarnessTest`

Atualmente:
1. `MainFrame.java` possui 1.190 linhas (+190 linhas acima do teto).
2. `ComercialPanel.java` possui 1.555 linhas (+555 linhas acima do teto).

## 2. Estratégia Canónica de Decomposição (SOLID / Single Responsibility)

### 2.1 Decomposição de `MainFrame.java`
- Extrair a barra de ferramentas de topo, ações de atalho, alternância de tema e zoom para um colaborador dedicado:
  `MainFrameToolbarBuilder.java` ou `MainFrameHeaderFactory.java`.
- Extrair os métodos utilitários de construção de menus/itens para um colaborador sem estado:
  `MainFrameNavigationBuilder.java`.
- Manter o `MainFrame.java` estritamente focado no ciclo de vida da janela (`JFrame`), troca de painéis (`CardLayout`) e escuta de eventos globais.
- Meta de linhas para `MainFrame.java`: <= 950 linhas.

### 2.2 Decomposição de `ComercialPanel.java`
- O `ComercialPanel.java` atualmente mistura:
  - Inicialização do painel e abas de navegação.
  - Controlador de Faturas e Rascunhos (`loadInvoicesPage`, `saveInvoiceFromEditor`, `syncInvoiceLineFromGrid`).
  - Controlador de Encomendas e Conversões (`loadOrdersPage`, `saveOrderFromEditor`).
  - Diálogos auxiliares (pesquisas, confirmações de faturação, cancelamentos).
- Extrair o controlador de faturas e sincronização da grelha de linhas para:
  `CommercialInvoiceActions.java` ou `CommercialInvoiceGridController.java`.
- Extrair o controlador de encomendas para colaborador dedicado:
  `CommercialOrderActions.java`.
- Meta de linhas para `ComercialPanel.java`: <= 950 linhas.

## 3. Harnesses de Validação
- `UiPanelDecompositionTest`
- `HighContrastThemeHarnessTest`
- `RecentItemsHistoryHarnessTest`
- `UiDensityZoomHarnessTest`
- `DesktopThinContextTest` (assegura que o contexto de injeção e arranque do Desktop permanece íntegro)

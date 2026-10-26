# SPEC-STOCK-WASTE-PANEL-DECOMPOSITION-001: Decomposição Modular do StockWastePanel

## 1. Contexto e Motivação
O painel de gestão de quebras, perdas e desperdício de stock ([StockWastePanel.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/StockWastePanel.java)) acumulou 955 linhas de código, agregando num único ficheiro:
1. Gestão e renderização de KPIs de perda acumulada e lotes em risco.
2. Aba de histórico e validação com tabela de 13 colunas, filtros cruzados por estado, motivo e período, e ações de aprovação/rejeição por alçada.
3. Aba de radar de validades e prevenção de desperdício com tabela de 10 colunas e classificação de urgência.
4. Aba de métricas analíticas e relatório executivo com tabelas de agregação por motivo e categoria, e impressão em PDF.
5. Diálogo modal volumoso de registo de quebras com cálculo de custo dinâmico e aviso de alçada (> 2.500 MT).

Para cumprir a arquitetura desacoplada e as metas de manutenção do sistema, este painel deve ser decomposto em componentes especializados com responsabilidade única.

---

## 2. Metas de Linhas e Distribuição de Responsabilidades

| Componente | Responsabilidade Principal | Limite Máximo | Meta Típica |
| :--- | :--- | :--- | :--- |
| **`StockWastePanel.java`** | Orquestrador principal, barra de KPIs, aba de histórico de quebras, e coordenação das abas secundárias. | $\le 380$ linhas | $\sim 340$ linhas |
| **`StockWasteRegistrationDialog.java`** | Diálogo modal ergonómico de registo de quebras com preenchimento a partir de lotes em risco, pré-visualização de custo e alçada. | $\le 200$ linhas | $\sim 160$ linhas |
| **`StockWasteRadarView.java`** | Aba "Radar de Validades & Prevenção" com tabela, filtros de urgência, inspeção rápida (`TableQuickPeekController`) e acionamento de abate. | $\le 190$ linhas | $\sim 150$ linhas |
| **`StockWasteMetricsView.java`** | Aba "Métricas & Relatório Executivo" com filtro de datas, tabelas de distribuição por motivo/categoria e emissão de PDF oficial. | $\le 180$ linhas | $\sim 140$ linhas |

---

## 3. Invariantes de Negócio e Contratos a Preservar
1. **Contrato de Construtor:**
   - Preservar rigorosamente o construtor público:
     `public StockWastePanel(StockWasteApiClient wasteApiClient, InventoryApiClient inventoryApiClient, ComercialApiClient comercialApiClient)`
2. **Propriedades Públicas e Package-Private auditadas por testes:**
   - `kpiTotalCost`, `kpiTotalQty`, `kpiWasteRate`, `kpiRiskBatches` (JLabels de topo).
   - `wasteTable` (13 colunas, altura de linha de 42px).
   - `radarTable` (10 colunas, altura de linha de 42px).
   - `periodFilterCombo` (filtro de período universal).
   - `currentWasteList`, `statusFilterCombo`, `reasonFilterCombo`.
   - `applyWasteFilters()`, `updateApprovalButtonsState()`, `approveBtn`, `rejectBtn`.
   - `reload()` e `reloadData()`.
3. **Ergonomia e Design System Multicore:**
   - Zero chamadas a `JOptionPane`. Modais sempre via `ModernFormDialog` ou `ToastManager`.
   - Ausência estrita de emojis em botões, abas ou cabeçalhos.
   - Tabelas contidas em `ModernPanel(16)` com `UIHelper.tableCardTop(...)`.
   - Consulta rápida em tabelas via tecla Espaço (`TableQuickPeekController`).
   - Alçada de quebra: perdas acima de 2.500,00 MT ficam em `PENDING_APPROVAL`.

---

## 4. Plano de Testes e Validação
- Executar `StockWastePanelHarnessTest` (todos os 8 casos de teste GQS-01 a GQS-08 devem permanecer 100% verdes).
- Criar e executar `StockWastePanelDecompositionHarnessTest` para validar limites de linhas, ausência de `JOptionPane` e integridade das classes desacopladas.
- Executar `DesktopThinContextTest` para garantir instanciação no arranque da aplicação desktop.
- Executar `MultiModuleArchitectureHarnessTest` no módulo backend para validar limites arquiteturais.

# SPEC-CASH-FLOW-FORECAST-PANEL-DECOMPOSITION-001: Decomposição Modular da Projeção de Fluxo de Caixa

## 1. Visão Geral e Objectivo
O painel `CashFlowForecastPanel.java` (411 linhas) é a central de tesouraria previsional e monitorização da matriz temporal de liquidez (horizontes vencidos, hoje, 7 dias, 30 dias, 60 dias e >90 dias), maiores recebimentos e pagamentos, alertas e emissão de relatório oficial em PDF.
O objetivo desta decomposição é desacoplar as abas de movimentos pendentes, a coordenação assíncrona de consulta e impressão em PDF e o renderizador monetário semafórico para colaboradores especializados de responsabilidade única, reduzindo o orquestrador para $\le 230$ linhas sem alterar qualquer comportamento ou contrato de teste pré-existente.

## 2. Invariantes Arquiteturais e Restrições Não Negociáveis
1. **Camadas Físicas**: Módulo `desktop` depende exclusivamente de `contracts` e consome serviços via HTTPS (`CashFlowForecastApiClient`). Proibido JPA, repositórios ou dependências circulares.
2. **Contratos Visuais e de Teste**:
   - Construtor público canónico: `public CashFlowForecastPanel(CashFlowForecastApiClient forecastApiClient)`.
   - Métodos públicos preservados para `CashFlowForecastPanelHarnessTest`:
     - `public void loadForecast()`
     - `public void displayForecast(CashFlowForecastDTO data)`
     - `public JTable getBucketsTable()`
     - `public ModernPanel getAlertBanner()`
     - `public CashFlowForecastDTO getCurrentForecast()`
   - Invariantes exigidos por `StatementsCashFlowUnifiedHarnessTest` (`STMT-03`):
     - Presença literal de `new ModernPanel(16)`
     - Ausência estrita de `new ModernPanel(10)`
     - Presença literal de `UIHelper.ensureHeadersFit(bucketsTable)`
   - Limite estrito de no máximo 4 KPIs via `KpiCard.createGrid(4)`.
   - Paginação artificial desativada na matriz fixa: `putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE)`.
   - Eliminação total de `JOptionPane`.
   - Formatação monetária estritamente em moeda moçambicana (`pt-MZ`).

## 3. Arquitetura de Componentes e Decomposição
A central de tesouraria previsional é estruturada em:

```
mz.multicore.erp.gui
├── CashFlowForecastPanel.java (Orquestrador UI: <= 230 linhas)
├── CashFlowForecastOperationsCoordinator.java (Operações assíncronas de consulta e emissão PDF: <= 85 linhas)
├── CashFlowForecastDetailsPanel.java (Card em abas com maiores recebimentos e pagamentos previstos: <= 95 linhas)
└── CashFlowMoneyCellRenderer.java (Renderizador semafórico de saldo cumulativo e movimento líquido: <= 45 linhas)
```

### 3.1 `CashFlowForecastOperationsCoordinator.java`
- Método `loadForecast(...)`: Executa `apiClient.getForecast()` via `SwingWorker` em segundo plano com indicação em `busyBar` e encaminha os dados para o callback consumidor.
- Método `printForecastPdf(...)`: Executa `apiClient.getForecastPdf()` e abre a pré-visualização oficial via `PrintPreviewDialog.show(parent, pdfBytes, "fluxo-caixa-previsional.pdf")`.
- Método `formatMoney(BigDecimal val)`: Formatação padronizada para o padrão moçambicano (`pt-MZ`).

### 3.2 `CashFlowForecastDetailsPanel.java`
- Painel corporativo baseado em `ModernPanel(16)` contendo `JTabbedPane` estilizado com `UIHelper.styleTabbedPaneMulticore`.
- Aba 1 ("Maiores Recebimentos Previstos"): Grelha tabular com documento, cliente, vencimento, montante pendente formatado e período, com suporte a Quick Peek.
- Aba 2 ("Maiores Pagamentos a Fornecedores"): Grelha tabular com documento, fornecedor, vencimento, montante pendente formatado e período, com suporte a Quick Peek.
- Métodos públicos `populate(List<CashFlowItemDTO> receivables, List<CashFlowItemDTO> payables)`.

### 3.3 `CashFlowMoneyCellRenderer.java`
- Extende `DefaultTableCellRenderer` com alinhamento à direita, formatação monetária em meticais (`pt-MZ`), negrito para saldo cumulativo e coloração semafórica (`REJECTED_RED` para valores negativos, `APPROVED_GREEN` para valores positivos).

## 4. Metas de Linhas de Código
| Ficheiro | Linhas Antes | Meta Após | Limite Máximo |
| :--- | :--- | :--- | :--- |
| `CashFlowForecastPanel.java` | 411 | ~215 | $\le 230$ |
| `CashFlowForecastOperationsCoordinator.java` | — | ~75 | $\le 85$ |
| `CashFlowForecastDetailsPanel.java` | — | ~85 | $\le 95$ |
| `CashFlowMoneyCellRenderer.java` | — | ~35 | $\le 45$ |

# HARNESS-CASH-FLOW-FORECAST-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness de Fluxo de Caixa Previsional

## 1. Identificação do Harness
- **ID**: `HARNESS-CASH-FLOW-FORECAST-PANEL-DECOMPOSITION-001`
- **Domínio**: Tesouraria & Fluxo de Caixa Previsional (`desktop/src/main/java/mz/multicore/erp/gui/CashFlowForecastPanel.java`)
- **Fase de Execução**: Fase 27

## 2. Casos de Teste do Harness

| ID | Teste | Alvo | Condição de Sucesso |
| :--- | :--- | :--- | :--- |
| **CFF-01** | `testLineCount` | `CashFlowForecastPanel.java` | Linhas totais $\le 230$ |
| **CFF-02** | `testCollaboratorsLineCount` | Novos colaboradores | `CashFlowForecastOperationsCoordinator.java` $\le 85$ linhas, `CashFlowForecastDetailsPanel.java` $\le 95$ linhas, `CashFlowMoneyCellRenderer.java` $\le 45$ linhas |
| **CFF-03** | `testCanonicalContractsPreserved` | `CashFlowForecastPanel.java` | Presença de `new ModernPanel(16)`, ausência de `new ModernPanel(10)`, presença de `UIHelper.ensureHeadersFit(bucketsTable)`, `KpiCard.createGrid(4)` |
| **CFF-04** | `testNoLegacyDialogs` | Todos os ficheiros | Zero ocorrências de `JOptionPane` |
| **CFF-05** | `testPublicApiPreserved` | `CashFlowForecastPanel.java` | Construtor `(CashFlowForecastApiClient)`, métodos `loadForecast()`, `displayForecast(CashFlowForecastDTO)`, `getBucketsTable()`, `getAlertBanner()`, `getCurrentForecast()` |

## 3. Matriz de Cobertura de Regressão
Para garantir zero regressão, devem ser validados os seguintes testes:
1. `CashFlowForecastPanelDecompositionHarnessTest`: (5/5)
2. `CashFlowForecastPanelHarnessTest`: (2/2)
3. `StatementsCashFlowUnifiedHarnessTest`: (3/3)
4. `KpiCardStandardizationTest`: (6/6)
5. `DesktopInitializationTest`: (1/1)
6. `DesktopThinContextTest`: (2/2)
7. `ProfessionalFeedbackHarnessTest`: (9/9)
8. `MultiModuleArchitectureHarnessTest`: (6/6)

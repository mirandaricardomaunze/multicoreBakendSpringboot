# HARNESS-RECURRING-CONTRACTS-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness de Contratos Recorrentes

## 1. Identificação do Harness
- **ID**: `HARNESS-RECURRING-CONTRACTS-PANEL-DECOMPOSITION-001`
- **Domínio**: Contratos & Facturação Recorrente (`desktop/src/main/java/mz/multicore/erp/gui/RecurringContractsPanel.java`)
- **Fase de Execução**: Fase 26

## 2. Casos de Teste do Harness

| ID | Teste | Alvo | Condição de Sucesso |
| :--- | :--- | :--- | :--- |
| **RCP-01** | `testLineCount` | `RecurringContractsPanel.java` | Linhas totais $\le 230$ |
| **RCP-02** | `testCollaboratorsLineCount` | Novos colaboradores | `RecurringContractFormDialog.java` $\le 135$ linhas, `RecurringContractsOperationsCoordinator.java` $\le 95$ linhas |
| **RCP-03** | `testCanonicalContractsPreserved` | `RecurringContractsPanel.java` | Presença de `new ModernPanel(16)`, `KpiCard.createGrid(4)`, `UIHelper.tableCardTop(`, `UIHelper.ensureHeadersFit(table)`, `TableFilter.install(` |
| **RCP-04** | `testNoLegacyDialogs` | Todos os ficheiros | Zero ocorrências de `JOptionPane` |
| **RCP-05** | `testPublicApiPreserved` | `RecurringContractsPanel.java` | Construtor `(RecurringContractApiClient)` e método `onPanelSelected()` |

## 3. Matriz de Cobertura de Regressão
Para garantir zero regressão, devem ser validados os seguintes testes:
1. `RecurringContractsPanelDecompositionHarnessTest`: (5/5)
2. `DesktopThinContextTest`: (2/2)
3. `DesktopInitializationTest`: (1/1)
4. `ProfessionalFeedbackHarnessTest`: (9/9)
5. `KpiCardStandardizationTest`: (6/6)
6. `MultiModuleArchitectureHarnessTest`: (6/6)

# HARNESS-APPROVALS-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Central de Aprovações

## 1. Identificação do Harness
- **ID**: `HARNESS-APPROVALS-PANEL-DECOMPOSITION-001`
- **Domínio**: Governança & Alçadas de Aprovação (`desktop/src/main/java/mz/multicore/erp/gui/ApprovalsPanel.java`)
- **Fase de Execução**: Fase 28

## 2. Casos de Teste do Harness

| ID | Teste | Alvo | Condição de Sucesso |
| :--- | :--- | :--- | :--- |
| **APP-01** | `testLineCount` | `ApprovalsPanel.java` | Linhas totais $\le 230$ |
| **APP-02** | `testCollaboratorsLineCount` | Novos colaboradores | `ApprovalsHistoryView.java` $\le 95$ linhas, `ApprovalsOperationsCoordinator.java` $\le 85$ linhas |
| **APP-03** | `testCanonicalContractsPreserved` | `ApprovalsPanel.java` | Presença de `UIHelper.ensureHeadersFit(pendingTable)`, `TableQuickPeekController.install(pendingTable`, `KpiCard.createGrid(4)`, `UIHelper.tableCardTop(` |
| **APP-04** | `testNoLegacyDialogs` | Todos os ficheiros | Zero ocorrências de `JOptionPane` |
| **APP-05** | `testOverloadedConstructorsPreserved` | `ApprovalsPanel.java` | Preservação dos construtores de 1, 3 e 4 argumentos |

## 3. Matriz de Cobertura de Regressão
Para garantir zero regressão, devem ser validados os seguintes testes:
1. `ApprovalsPanelDecompositionHarnessTest`: (5/5)
2. `AdminApprovalsUnifiedHarnessTest`: (3/3)
3. `TableActionPlacementHarnessTest`: (6/6)
4. `DesktopInitializationTest`: (1/1)
5. `DesktopThinContextTest`: (2/2)
6. `ProfessionalFeedbackHarnessTest`: (9/9)
7. `MultiModuleArchitectureHarnessTest`: (6/6)

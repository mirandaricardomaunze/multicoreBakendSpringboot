# HARNESS-FORENSIC-AUDIT-PANEL-DECOMPOSITION-001: Matriz de Validação & Harness da Auditoria Forense

## 1. Identificação do Harness
- **ID**: `HARNESS-FORENSIC-AUDIT-PANEL-DECOMPOSITION-001`
- **Domínio**: Auditoria Forense & Controlo de Fraude (`desktop/src/main/java/mz/multicore/erp/gui/ForensicAuditPanel.java`)
- **Fase de Execução**: Fase 25

## 2. Casos de Teste do Harness

| ID | Teste | Alvo | Condição de Sucesso |
| :--- | :--- | :--- | :--- |
| **FAP-01** | `testLineCount` | `ForensicAuditPanel.java` | Linhas totais $\le 280$ |
| **FAP-02** | `testCollaboratorsLineCount` | Novos colaboradores | `ForensicAnomalyDetailsDialog.java` $\le 75$ linhas, `ForensicAuditOperationsCoordinator.java` $\le 115$ linhas, `ForensicCellRenderer.java` $\le 50$ linhas |
| **FAP-03** | `testCanonicalContractsPreserved` | `ForensicAuditPanel.java` | Presença de `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`, `new ModernPanel(16)`, `KpiCard.createGrid(4)` |
| **FAP-04** | `testNoLegacyDialogs` | Todos os ficheiros | Zero ocorrências de `JOptionPane` |
| **FAP-05** | `testNoNewColorAndNoForbiddenWords` | `ForensicAuditPanel.java` | Zero `new Color(`, zero `atualizar` (case-insensitive) |
| **FAP-06** | `testPublicAndPackageApiPreserved` | `ForensicAuditPanel.java` | Construtor `(ForensicAuditApiClient)`, campos `kpiTotalRisk`, `kpiCritical`, `kpiSuspicious`, `kpiComplianceScore`, `startDateField`, `endDateField`, `severityCombo`, `categoryCombo`, `operatorField`, `table`, `tableModel` e método `renderSummary` |

## 3. Matriz de Cobertura de Regressão
Para garantir zero regressão, devem ser validados os seguintes testes:
1. `ForensicAuditPanelHarnessTest`: (3/3)
2. `AccountingFiscalUnifiedHarnessTest`: (3/3)
3. `DesktopInitializationTest`: (1/1)
4. `SellerLeanStartupHarnessTest`: (2/2)
5. `ForensicAuditPanelDecompositionHarnessTest`: (6/6)
6. `DesktopThinContextTest`: (2/2)
7. `MultiModuleArchitectureHarnessTest`: (6/6)

# Plano de Testes & Validação Arquitetural: Decomposição do Painel de Risco de Crédito

**Documento:** `HARNESS-CREDIT-RISK-PANEL-DECOMPOSITION-002`  
**Data:** 2026-10-24  
**Harness Alvo:** `mz.multicore.erp.gui.CreditRiskPanelDecompositionHarnessTest`  
**Referência:** `SPEC-CREDIT-RISK-PANEL-DECOMPOSITION-002`  

---

## 1. Casos de Teste do Harness

| ID | Descrição do Caso de Teste | Critério de Sucesso |
|---|---|---|
| **CRD-01** | Redução Física de Linhas | `CreditRiskPanel.java` possui $\le 165$ linhas. |
| **CRD-02** | Desacoplamento dos Colaboradores | `CreditRiskViewCoordinator.java` ($\le 110$ linhas), `CreditRiskOperationsCoordinator.java` e `CreditRiskCellRenderer.java` instanciáveis e funcionais. |
| **CRD-03** | Preservação de Campos e Contratos de Testes | Campos package-private `kpiTotalReceivable`, `kpiTotalOverdue`, `kpiBlockedCount`, `kpiCriticalCount`, `table`, `tableModel`, `dateField`, `riskFilterCombo`, `searchField`, `allClientsRisk`, `visibleClientsRisk` preservados. |
| **CRD-04** | Erradicação de `JOptionPane` | 0 ocorrências de `JOptionPane` em `CreditRiskPanel.java`, `CreditRiskViewCoordinator.java`, `CreditExceptionDialog.java` e `CreditRiskOperationsCoordinator.java`. |
| **CRD-05** | Conformidade de Topo de Card e QuickPeek | Presença comprovada de `UIHelper.tableCardTop("Matriz de Aging & Gestão de Risco"`, `TableQuickPeekController.install(table` e `UIHelper.ensureHeadersFit(table)`. |

---

## 2. Matriz de Cobertura de Testes Existentes

1. `CreditRiskPanelDecompositionHarnessTest` (5/5)
2. `CreditRiskPanelHarnessTest` (7/7)
3. `AdminApprovalsUnifiedHarnessTest` (3/3)
4. `TableActionPlacementHarnessTest` (6/6)
5. `KpiCardStandardizationTest` (1/1)
6. `DesktopInitializationTest` (1/1)
7. `DesktopThinContextTest` (2/2)
8. `MultiModuleArchitectureHarnessTest` (6/6)
9. `ProfessionalFeedbackHarnessTest` (9/9)

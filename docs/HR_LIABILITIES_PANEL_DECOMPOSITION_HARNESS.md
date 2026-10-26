# Harness de Testes & Validação — Decomposição do HRLiabilitiesPanel

**ID:** `HARNESS-HR-LIABILITIES-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-HR-LIABILITIES-PANEL-DECOMPOSITION-001`  
**Alvo:** `mz.multicore.erp.gui.HRLiabilitiesPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivos do Harness

1. **Garantia de Tetos de Linhas:**
   - `HRLiabilitiesPanel.java` $\le 185$ linhas.
   - `HrPolicyConfigDialog.java` $\le 85$ linhas.
   - `HrLiabilityDeliveryDialog.java` $\le 55$ linhas.
   - `HRLiabilitiesOperationsCoordinator.java` $\le 75$ linhas.

2. **Invariantes Arquiteturais e de Layout:**
   - Conformidade com `HRUnifiedHarnessTest` (HR-03): presença de `UIHelper.ensureHeadersFit(table)` e `TableQuickPeekController.install(table`.
   - Conformidade com `HrCrmUiErgonomicsHarnessTest`: presença de `UIHelper.tableCardTop("Retenções por Entregar", bar,`, `refreshBtn, actionsBtn, deliverBtn)`, `"Apurar Período"` e `"Valores Legais"`.
   - Conformidade com `TableActionPlacementHarnessTest`: presença de `UIHelper.tableCardTop(`.
   - Manutenção de 0 chamadas a `JOptionPane` em todos os ficheiros criados/modificados, e avanço da catraca no `ProfessionalFeedbackHarnessTest`.

3. **Verificação de Regressão e Contexto:**
   - Suíte unitária `HRLiabilitiesPanelDecompositionHarnessTest`.
   - `HRUnifiedHarnessTest`, `HrCrmUiErgonomicsHarnessTest`, `TableActionPlacementHarnessTest`, `ProfessionalFeedbackHarnessTest`.
   - `DesktopInitializationTest` e `DesktopThinContextTest`.
   - Reactor Maven `MultiModuleArchitectureHarnessTest` em `backend`.

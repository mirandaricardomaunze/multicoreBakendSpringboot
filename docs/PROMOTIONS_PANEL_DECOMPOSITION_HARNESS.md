# Harness de Testes & Validação — Decomposição do PromotionsPanel

**ID:** `HARNESS-PROMOTIONS-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-PROMOTIONS-PANEL-DECOMPOSITION-001`  
**Alvo:** `mz.multicore.erp.gui.PromotionsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivos do Harness

1. **Garantia de Tetos de Linhas:**
   - `PromotionsPanel.java` $\le 175$ linhas.
   - `PromotionFormDialog.java` $\le 115$ linhas.
   - `PromotionsOperationsCoordinator.java` $\le 65$ linhas.

2. **Invariantes Arquiteturais e de Layout:**
   - Conformidade com `CommercialCrmUnifiedHarnessTest` (COMM-01): presença de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table` e proibição de `promoFilters = new JPanel(new GridBagLayout())`.
   - Conformidade com `TableActionPlacementHarnessTest`: presença de `UIHelper.tableCardTop(`.
   - Manutenção de 0 chamadas a `JOptionPane`.

3. **Verificação de Regressão e Contexto:**
   - Suíte unitária `PromotionsPanelDecompositionHarnessTest`.
   - `CommercialCrmUnifiedHarnessTest`, `TableActionPlacementHarnessTest`, `ProfessionalFeedbackHarnessTest`.
   - `DesktopInitializationTest` e `DesktopThinContextTest`.
   - Reactor Maven `MultiModuleArchitectureHarnessTest` em `backend`.

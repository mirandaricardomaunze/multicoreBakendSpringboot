# Harness de Testes & Validação — Decomposição do OutstandingAccountsPanel

**ID:** `HARNESS-OUTSTANDING-ACCOUNTS-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-OUTSTANDING-ACCOUNTS-PANEL-DECOMPOSITION-001`  
**Alvo:** `mz.multicore.erp.gui.commercial.OutstandingAccountsPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.commercial`)  

---

## 1. Objectivos do Harness

1. **Garantia de Tetos de Linhas:**
   - `OutstandingAccountsPanel.java` $\le 205$ linhas.
   - `OutstandingPaymentDialog.java` $\le 90$ linhas.
   - `OutstandingAccountsOperationsCoordinator.java` $\le 115$ linhas.

2. **Invariantes Arquiteturais e de Layout:**
   - Conformidade com `CommercialCrmUnifiedHarnessTest` (COMM-02): presença de `TableFilter.bar(`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table`, proibição de `new JPanel(new GridBagLayout())`.
   - Conformidade com `TableActionPlacementHarnessTest`: presença de `UIHelper.tableCardTop(`.
   - Manutenção de 0 chamadas a `JOptionPane` em todos os colaboradores e painel.
   - Preservação do `ActionMenuButton` contendo "Radar Preventivo", "Prestações" e "Devolver Fatura".

3. **Verificação de Regressão e Contexto:**
   - Suíte unitária `OutstandingAccountsPanelDecompositionHarnessTest`.
   - `DesktopInitializationTest` e `DesktopThinContextTest`.
   - Reactor Maven `MultiModuleArchitectureHarnessTest` em `backend`.

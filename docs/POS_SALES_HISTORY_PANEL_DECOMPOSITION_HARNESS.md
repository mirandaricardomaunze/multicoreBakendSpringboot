# Plano de Testes & Validação: Decomposição de PosSalesHistoryPanel

**Identificador:** `HARNESS-POS-SALES-HISTORY-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/POS_SALES_HISTORY_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração modular do painel `PosSalesHistoryPanel` reduz a sua extensão física de 293 linhas para $\le 165$ linhas sem quebrar nenhum dos contratos literais exigidos por `CommercialCrmUnifiedHarnessTest` (COMM-03), `BrightButtonsHarnessTest`, `PrintModalHarnessTest`, `ProfessionalFeedbackHarnessTest` e `DesktopInitializationTest`.

---

## 2. Casos de Teste do Harness (`PosSalesHistoryPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `PosSalesHistoryPanel.java` possui $\le 165$ linhas.
2. **`testCollaboratorLineCount`:**
   - Valida que `PosSalesHistoryViewCoordinator.java` possui $\le 135$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `PosSalesHistoryPanel.java` contém:
     - `TableFilter.bar(`
     - `UIHelper.ensureHeadersFit(owner.salesHistoryTable)`
     - `new ModernPanel(16)`
     - `new TablePager(`
     - Não contém `shBar = new JPanel(new GridBagLayout())`
4. **`testBrightButtonsCompliance`:**
   - Valida que `PosSalesHistoryPanel.java` e `PosSalesHistoryViewCoordinator.java` não contêm `createSecondaryButton` nem `BUTTON_NEUTRAL`.
5. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
6. **`testPublicApiPreserved`:**
   - Valida a existência do construtor package-private `PosSalesHistoryPanel(POSPanel owner)`.
   - Valida a existência dos métodos públicos `buildPanel()` e `refresh()`.

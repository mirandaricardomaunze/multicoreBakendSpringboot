# Plano de Testes & Validação: Decomposição de HRTerminationsPanel

**Identificador:** `HARNESS-HR-TERMINATIONS-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/HR_TERMINATIONS_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração do painel `HRTerminationsPanel` reduz o seu número de linhas sem quebrar nenhum dos contratos literais esperados por `HrCrmUiErgonomicsHarnessTest`, `HRUnifiedHarnessTest`, `TableActionPlacementHarnessTest` ou `ProfessionalFeedbackHarnessTest`.

---

## 2. Casos de Teste do Harness (`HRTerminationsPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `HRTerminationsPanel.java` possui $\le 165$ linhas.
2. **`testCollaboratorsLineCount`:**
   - Valida que `HrTerminationFormDialog.java` possui $\le 135$ linhas.
   - Valida que `HRTerminationsOperationsCoordinator.java` possui $\le 90$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `HRTerminationsPanel.java` contém:
     - `new ModernPanel(16)`
     - `UIHelper.tableCardTop("Cessações e Acertos Finais", bar,`
     - `docsBtn, payBtn, newBtn)`
     - `Acerto PDF`
     - `Certificado de Trabalho`
     - `TableQuickPeekController.install(table`
     - `UIHelper.ensureHeadersFit(table)`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
5. **`testConstructorPreserved`:**
   - Valida a existência do construtor package-private `HRTerminationsPanel(HRPanel.class)`.

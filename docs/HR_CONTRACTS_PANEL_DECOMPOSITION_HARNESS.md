# Plano de Testes & Validação: Decomposição de HRContractsPanel

**Identificador:** `HARNESS-HR-CONTRACTS-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/HR_CONTRACTS_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração do painel `HRContractsPanel` reduz o seu número de linhas sem quebrar nenhum dos contratos literais esperados por `HRContractsPanelHarnessTest`, `HRUnifiedHarnessTest`, `TableActionPlacementHarnessTest` ou `ProfessionalFeedbackHarnessTest`.

---

## 2. Casos de Teste do Harness (`HRContractsPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `HRContractsPanel.java` possui $\le 175$ linhas.
2. **`testCollaboratorsLineCount`:**
   - Valida que `HrContractFormDialog.java` possui $\le 140$ linhas.
   - Valida que `HRContractsOperationsCoordinator.java` possui $\le 80$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `HRContractsPanel.java` contém:
     - `new ModernPanel(16)`
     - `UIHelper.tableCardTop("Contratos de Trabalho"`
     - `TableQuickPeekController.install(table`
     - `UIHelper.ensureHeadersFit(table)`
     - `UIHelper.createActionMenuButton("Gestão do Contrato")`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
5. **`testConstructorPreserved`:**
   - Valida a existência do construtor package-private `HRContractsPanel(HRPanel.class)`.

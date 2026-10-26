# HARNESS-HR-DEDUCTIONS-PANEL-DECOMPOSITION-001: Matriz de Testes e Validação do Painel de Descontos

## 1. Critérios de Aceitação & Catracas
1. **Linhas de Código:**
   - `HRDeductionsPanel.java` $\le 140$ linhas (anterior: 278 linhas).
   - `HrDeductionFormDialog.java` $\le 150$ linhas.
2. **Zero `JOptionPane`:**
   - Zero ocorrências em `HRDeductionsPanel.java` e `HrDeductionFormDialog.java`.
   - Remoção de `HRDeductionsPanel.java` da allowlist de tolerância do `ProfessionalFeedbackHarnessTest.java`.
3. **Contratos Canónicos Preservados:**
   - `UIHelper.tableCardTop("Descontos, Adiantamentos e Empréstimos", bar,`
   - `refreshBtn, stopBtn, newDeductionBtn)`
   - `Novo Desconto`
   - `Adiantamento Salarial`, `Empréstimo`, `Desconto Recorrente`
   - `UIHelper.ensureHeadersFit(table)`
   - `TableQuickPeekController.install(table`

## 2. Testes de Regressão e Homologação
- `HRDeductionsPanelDecompositionHarnessTest` (novo conjunto unitário).
- `HRUnifiedHarnessTest` (`HR-02`).
- `HrCrmUiErgonomicsHarnessTest`.
- `TableActionPlacementHarnessTest`.
- `ProfessionalFeedbackHarnessTest`.
- `DesktopInitializationTest`.
- `DesktopThinContextTest`.
- `MultiModuleArchitectureHarnessTest`.

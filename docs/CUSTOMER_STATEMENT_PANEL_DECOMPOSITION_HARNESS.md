# Plano de Testes & Validação: Decomposição de CustomerStatementPanel

**Identificador:** `HARNESS-CUSTOMER-STATEMENT-PANEL-DECOMPOSITION-001`  
**Referência:** `docs/CUSTOMER_STATEMENT_PANEL_DECOMPOSITION_SPEC.md`  

---

## 1. Objectivos do Harness

Garantir que a refatoração do painel `CustomerStatementPanel` reduz o seu número de linhas sem quebrar nenhum dos contratos literais esperados por `StatementsCashFlowUnifiedHarnessTest`, `AdminApprovalsUnifiedHarnessTest` ou `AccountStatementPanelHarnessTest`.

---

## 2. Casos de Teste do Harness (`CustomerStatementPanelDecompositionHarnessTest`)

1. **`testLineCount`:**
   - Valida que `CustomerStatementPanel.java` possui $\le 210$ linhas.
2. **`testCollaboratorLineCount`:**
   - Valida que `CustomerStatementOperationsCoordinator.java` possui $\le 165$ linhas.
3. **`testCanonicalContractsPreserved`:**
   - Valida que `CustomerStatementPanel.java` contém:
     - `new ModernPanel(16)`
     - `UIHelper.tableCardTop("Conta Corrente & Reconciliação de Clientes"`
     - `TableQuickPeekController.install(table`
     - `UIHelper.ensureHeadersFit(table)`
     - `ClientTablePagination.install(table)`
     - `KpiCard.createGrid(5)`
4. **`testNoLegacyDialogs`:**
   - Valida que nenhum dos ficheiros contém chamadas a `JOptionPane`.
5. **`testPublicApiPreserved`:**
   - Valida a existência do construtor canónico de 2 argumentos.
   - Valida a existência da classe aninhada pública `CustomerStatementPanel.ClientComboItem`.

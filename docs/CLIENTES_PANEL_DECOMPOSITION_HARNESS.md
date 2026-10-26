# Plano de Testes & Validação — Decomposição do ClientesPanel

**ID:** `HARNESS-CLIENTES-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-CLIENTES-PANEL-DECOMPOSITION-001`  
**Test Suite Dedicado:** `mz.multicore.erp.gui.ClientesPanelDecompositionHarnessTest`  

---

## 1. Objectivo do Harness

Garantir que a refactoração de `ClientesPanel.java` cumpre integralmente os requisitos de separação de responsabilidades, tetos de linhas, preservação de contratos de API e compatibilidade com os testes de regressão existentes no repositório.

---

## 2. Casos de Teste Estruturais e Funcionais

1. **`testTetoDeLinhasClientesPanel`:**
   - Verifica que `ClientesPanel.java` possui $\le 245$ linhas.
   - Verifica que `ClientFormDialog.java` possui $\le 100$ linhas.
   - Verifica que `ClientesOperationsCoordinator.java` possui $\le 100$ linhas.

2. **`testEliminacaoDeJOptionPane`:**
   - Garante que `ClientesPanel.java` não contém chamadas directas a `JOptionPane.`.
   - As confirmações de exclusão devem usar `UIHelper.confirm`.

3. **`testPreservacaoDeContratosDeLayoutEExportacao`:**
   - Garante a presença de `UIHelper.tableCardTop("Gestão de Clientes"`.
   - Garante a presença de `ClientTablePagination.install(table)`.
   - Garante a presença de `UIHelper.ensureHeadersFit(table)`.
   - Garante a presença de `TableQuickPeekController.install(table`.
   - Garante a presença de `TableFilter.bar(`.
   - Garante a presença de `TableExportAction.export(this, printApiClient, table, "Clientes", "clientes")`.

4. **`testPreservacaoDeAbasEConstrutores`:**
   - Verifica que a classe mantém os dois construtores sobrecarregados (3 e 4 argumentos).
   - Verifica a presença dos métodos canónicos `selectCreditRiskTab()` e `selectStatementTab()`.

---

## 3. Matriz de Regressão Obrigatória

Após a implementação, os seguintes testes devem ser executados com sucesso:
- `ClientesPanelDecompositionHarnessTest`
- `AccountStatementPanelHarnessTest`
- `RadarAlertasProactivoHarnessTest`
- `CommercialCrmUnifiedHarnessTest`
- `AdminApprovalsUnifiedHarnessTest`
- `UniversalTablePaginationHarnessTest`
- `TableActionPlacementHarnessTest`
- `TableExportFilterTest`
- `ProfessionalFeedbackHarnessTest`
- `DesktopInitializationTest`
- `DesktopThinContextTest`
- `MultiModuleArchitectureHarnessTest`

# Plano de Testes & Validação — Decomposição do CommercialMovementsPanel

**ID:** `HARNESS-COMMERCIAL-MOVEMENTS-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-COMMERCIAL-MOVEMENTS-PANEL-DECOMPOSITION-001`  
**Test Suite Dedicado:** `mz.multicore.erp.gui.commercial.CommercialMovementsPanelDecompositionHarnessTest`  

---

## 1. Objectivo do Harness

Garantir que a refactoração de `CommercialMovementsPanel.java` cumpre integralmente os requisitos de separação de responsabilidades, tetos de linhas, preservação de contratos de API e compatibilidade com os testes de regressão existentes no repositório.

---

## 2. Casos de Teste Estruturais e Funcionais

1. **`testTetoDeLinhasCommercialMovementsPanel`:**
   - Verifica que `CommercialMovementsPanel.java` possui $\le 225$ linhas.
   - Verifica que `CommercialMovementsDataCoordinator.java` possui $\le 150$ linhas.
   - Verifica que `CommercialMovementsOperationsCoordinator.java` possui $\le 90$ linhas.

2. **`testPreservacaoDeContratosDeLayoutEExportacao`:**
   - Garante a presença de `UIHelper.tableCardTop(`.
   - Garante a presença de `TableFilter.bar(`.
   - Garante a presença de `UIHelper.ensureHeadersFit(table)`.
   - Garante a presença de `TableQuickPeekController.install(table`.
   - Garante a presença de `ClientTablePagination.install(table)`.
   - Garante ausência de `new JPanel(new GridBagLayout())`.

3. **`testPreservacaoDeConstrutores`:**
   - Verifica que a classe mantém os 3 construtores sobrecarregados (1, 2 e 3 parâmetros).

---

## 3. Matriz de Regressão Obrigatória

Após a implementação, os seguintes testes devem ser executados com sucesso:
- `CommercialMovementsPanelDecompositionHarnessTest`
- `CommercialCrmUnifiedHarnessTest`
- `TableActionPlacementHarnessTest`
- `DesktopInitializationTest`
- `DesktopThinContextTest`
- `MultiModuleArchitectureHarnessTest`

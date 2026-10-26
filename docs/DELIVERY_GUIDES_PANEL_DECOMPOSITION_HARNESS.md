# Plano de Testes & Validação — Decomposição do DeliveryGuidesPanel

**ID:** `HARNESS-DELIVERY-GUIDES-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-DELIVERY-GUIDES-PANEL-DECOMPOSITION-001`  
**Test Suite Dedicado:** `mz.multicore.erp.gui.commercial.DeliveryGuidesPanelDecompositionHarnessTest`  

---

## 1. Objectivo do Harness

Garantir que a refactoração de `DeliveryGuidesPanel.java` cumpre integralmente os requisitos de separação de responsabilidades, tetos de linhas, eliminação completa de `JOptionPane`, preservação de contratos de API e compatibilidade com os testes de regressão existentes no repositório.

---

## 2. Casos de Teste Estruturais e Funcionais

1. **`testTetoDeLinhasDeliveryGuidesPanel`:**
   - Verifica que `DeliveryGuidesPanel.java` possui $\le 250$ linhas.
   - Verifica que `DeliveryGuidesOperationsCoordinator.java` possui $\le 175$ linhas.

2. **`testEliminacaoDeJOptionPane`:**
   - Garante que `DeliveryGuidesPanel.java` e `DeliveryGuidesOperationsCoordinator.java` possuem 0 chamadas a `JOptionPane.`.

3. **`testPreservacaoDeContratosDeLayoutEAcoes`:**
   - Garante a presença de `ActionMenuButton`.
   - Garante a presença de `"Aprovar"`, `"Rejeitar"`, `"Cancelar"`.
   - Garante a presença de `createRefreshButton(this::refresh)`.
   - Garante a presença de `TableFilter.bar(`.
   - Garante a presença de `UIHelper.ensureHeadersFit(table)`.
   - Garante a presença de `TableQuickPeekController.install(table`.
   - Garante a presença de `PeriodFilter` e `periodCombo`.
   - Garante a presença de `UIHelper.tableCardTop(`.

4. **`testPreservacaoDeConstrutores`:**
   - Verifica que a classe mantém os 4 construtores sobrecarregados (2, 3, 5 e 6 parâmetros).

---

## 3. Matriz de Regressão Obrigatória

Após a implementação, os seguintes testes devem ser executados com sucesso:
- `DeliveryGuidesPanelDecompositionHarnessTest`
- `DeliveryGuidesShortcutTest`
- `StockCommercialUiHarnessTest`
- `CommercialOperacionalUnifiedHarnessTest`
- `CommercialMultiuserRefreshHarnessTest`
- `TableActionPlacementHarnessTest`
- `TableFilterHarnessTest`
- `ProfessionalFeedbackHarnessTest`
- `DesktopInitializationTest`
- `DesktopThinContextTest`
- `MultiModuleArchitectureHarnessTest`

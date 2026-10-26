# Plano de Testes & Validação — Decomposição do FinanceiroPanel

**ID:** `HARNESS-FINANCEIRO-PANEL-DECOMPOSITION-001`  
**Referência:** `SPEC-FINANCEIRO-PANEL-DECOMPOSITION-001`  
**Test Suite Dedicado:** `mz.multicore.erp.gui.FinanceiroPanelDecompositionHarnessTest`  

---

## 1. Objectivo do Harness

Garantir que a refactoração de `FinanceiroPanel.java` cumpre integralmente os requisitos de separação de responsabilidades, tetos de linhas, preservação de contratos de API e compatibilidade com os testes de regressão existentes no repositório.

---

## 2. Casos de Teste Estruturais e Funcionais

1. **`testTetoDeLinhasFinanceiroPanel`:**
   - Verifica que `FinanceiroPanel.java` possui $\le 230$ linhas.
   - Verifica que `TreasuryReceiptDialog.java` possui $\le 85$ linhas.
   - Verifica que `TreasuryOperationsCoordinator.java` possui $\le 100$ linhas.

2. **`testPreservacaoDeContratosDeLayoutEFiltro`:**
   - Garante a presença de `PeriodFilter`.
   - Garante a presença de `periodCombo`.
   - Garante a presença de `UIHelper.tableCardTop("Contas de Tesouraria & Saldos"`.
   - Garante a presença de `UIHelper.tableCardTop("Histórico de Fluxo de Caixa"`.
   - Garante a presença de `UIHelper.ensureHeadersFit(`.
   - Garante a presença de `TableQuickPeekController.install(`.

3. **`testPreservacaoDeMetodosEConstrutores`:**
   - Verifica que a classe mantém os dois construtores sobrecarregados (3 e 4 parâmetros).
   - Verifica a presença de `getForecastPanel()`, `showForecastTab()` e `showReconciliationTab()`.

---

## 3. Matriz de Regressão Obrigatória

Após a implementação, os seguintes testes devem ser executados com sucesso:
- `FinanceiroPanelDecompositionHarnessTest`
- `CashFlowForecastPanelHarnessTest`
- `TableFilterHarnessTest`
- `ProfessionalFeedbackHarnessTest`
- `DesktopInitializationTest`
- `DesktopThinContextTest`
- `MultiModuleArchitectureHarnessTest`

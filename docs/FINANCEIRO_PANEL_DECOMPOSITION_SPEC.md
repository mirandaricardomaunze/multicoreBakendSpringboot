# Especificação Canónica — Decomposição do FinanceiroPanel

**ID:** `SPEC-FINANCEIRO-PANEL-DECOMPOSITION-001`  
**Domínio:** Finanças / Tesouraria & Fluxo de Caixa  
**Alvo:** `mz.multicore.erp.gui.FinanceiroPanel`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  

---

## 1. Objectivo & Diagnóstico

O `FinanceiroPanel` é a central de tesouraria do Multicore ERP, integrando a gestão de contas de tesouraria, fluxo de caixa operacional, reconciliação bancária (`BankReconciliationPanel`) e projeção previsional (`CashFlowForecastPanel`).

### Diagnóstico de Complexidade
- **Acúmulo de Responsabilidades:** O painel acumulava montagem de 4 abas especializadas, duas tabelas com formatações e quick peeks dedicados, carregamento paginado assíncrono com `TablePager`, diálogo modal completo de liquidação de faturas de clientes com combos estilizados e validação assíncrona (`registerReceipt`), e conversão de transações para linhas do modelo.
- **Tamanho original:** 342 linhas de código.

---

## 2. Arquitectura & Colaboradores Especializados

A decomposição segue o padrão corporativo canónico:

1. **`FinanceiroPanel` (Orquestrador / View Principal):**
   - Gestão de ciclo de vida das abas corporativas (`tabbedPane`), alternância de abas especializadas (`showReconciliationTab`, `showForecastTab`).
   - Construção dos cards principais `ModernPanel(16)` contendo filtros canónicos em linha única via `TableFilter.bar`, paginação remota (`TablePager`), e desativação de paginação artificial client-side (`ClientTablePagination.DISABLED`).
   - Meta de linhas: $\le 230$ linhas.

2. **`TreasuryReceiptDialog` (`mz.multicore.erp.gui`):**
   - Diálogo modal ergonómico e assíncrono baseado em `ModernFormDialog` para liquidação de faturas pendentes de recebimento em contas de tesouraria.
   - Formatação clara de valores em Meticais, verificação defensiva de faturas/contas vazias com `InlineFeedbackPanel`, e feedback visual com `ToastManager`.

3. **`TreasuryOperationsCoordinator` (`mz.multicore.erp.gui`):**
   - Configuração dinâmica de Quick Peek para contas de tesouraria (`populateAccountsPeek`) e transações de fluxo de caixa (`populateMovementsPeek`).
   - Povoamento e mapeamento das linhas de transações paginadas e contas de tesouraria com conversão de tipos (CAIXA / BANCO, DÉBITO / CRÉDITO com inversão de sinal).

---

## 3. Invariantes de Contrato e Layout Obrigatórios

- **Construtores Canónicos:** Preservação estrita dos construtores sobrecarregados:
  - `FinanceiroPanel(FinanceApiClient, ComercialApiClient, BankReconciliationApiClient)`
  - `FinanceiroPanel(FinanceApiClient, ComercialApiClient, BankReconciliationApiClient, CashFlowForecastApiClient)`
- **Contratos de Teste Existentes:**
  - `CashFlowForecastPanelHarnessTest`: Preservação de `getForecastPanel()`, `showForecastTab()` e `showReconciliationTab()`.
  - `TableFilterHarnessTest`: Presença obrigatória de `PeriodFilter` e `periodCombo`.
  - `ProfessionalFeedbackHarnessTest`: Manutenção estrita de 0 chamadas a `JOptionPane`.
  - Contratos de reflexão e acessibilidade: Preservação de `onPanelSelected()` e `refreshData()`.

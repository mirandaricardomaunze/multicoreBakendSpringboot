# HARNESS-BANK-RECONCILIATION-PANEL-DECOMPOSITION-001: Critérios de Homologação e Testes Automatizados

## 1. Objectivo

Este documento estabelece os critérios rigorosos de aceitação para homologar a Fase 8 de decomposição modular do painel `BankReconciliationPanel.java`.

---

## 2. Critérios de Homologação

### 2.1. Tetos Físicos de Linhas de Código
- `BankReconciliationPanel.java`: $\le 450$ linhas.
- `BankStatementImportDialog.java`: $\le 190$ linhas.
- `BankReconciliationMatchDialogs.java`: $\le 190$ linhas.
- `BankReconciliationOperationsCoordinator.java`: $\le 160$ linhas.

### 2.2. Eliminação de Legacy Dialogs
- Zero chamadas a `JOptionPane` em qualquer ficheiro do domínio de reconciliação bancária.
- Todos os modais usam `ModernFormDialog`, `ModernMessageDialog` ou `ToastManager`.

### 2.3. Conformidade Ergonómica & Invariantes de Teste
- `accountCombo`, `statementCombo` e `operationsMenu` rigorosamente contidos dentro do card `ModernPanel(16)`.
- `mainContent` contém exactamente 4 componentes: `feedback`, `kpis`, espaçador vertical e `tableCard`.
- `operationsMenu` possui no máximo 5 acções operacionais.
- QuickPeek silencioso acionado pela tecla Espaço na tabela `itemsTable`.
- KPIs interativos com drilldown para "PENDENTE" e reposição para "Todos os estados".
- Toasts de aviso com mensagens canónicas quando não há linha selecionada:
  - Desconciliação: "Seleccione um movimento bancário reconciliado..."
  - Reconciliação manual: "Seleccione um item do extracto pendente..."
  - Despesa bancária: "Seleccione a linha do extracto..."

---

## 3. Matriz de Testes Automatizados

1. **`BankReconciliationPanelDecompositionHarnessTest.java`**:
   - `testFileLinesWithinLimits`: Verifica que nenhum ficheiro ultrapassa o seu teto.
   - `testZeroJOptionPaneUsage`: Garante zero `JOptionPane` em todos os ficheiros.
   - `testPanelInstantiationAndComponents`: Verifica integridade dos componentes Swing no EDT.
2. **`BankReconciliationUnifiedHarnessTest.java`**:
   - Validação dos 5 cenários `TRECON-01` a `TRECON-05`.
3. **`GuiActionAlertGuidanceHarnessTest.java`**:
   - Validação do teste `ALERT-06`.
4. **`DesktopThinContextTest.java`**:
   - Verificação do arranque desktop sem headless e sem exceções.
5. **`MultiModuleArchitectureHarnessTest.java`**:
   - Isolamento das camadas `contracts`, `backend` e `desktop`.

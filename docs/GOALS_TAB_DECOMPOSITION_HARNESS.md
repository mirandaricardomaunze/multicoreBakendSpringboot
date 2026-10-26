# Harness de Testes — Decomposição Modular de GoalsTab (Fase 66)

## 1. Suíte de Verificação

Arquivo de teste criado: `desktop/src/test/java/mz/multicore/erp/gui/performance/GoalsTabDecompositionHarnessTest.java`.

## 2. Casos de Teste Rigorosos

1. **`GTD-01: Limites físicos estritos de linhas de código`:**
   - `GoalsTab.java` $\le 165$ linhas (redução drástica de 478 linhas).
   - `GoalCreateDialog.java` $\le 210$ linhas.
2. **`GTD-02: Zero diálogos legados JOptionPane`:**
   - Nenhum uso de `JOptionPane.show*` em `GoalsTab.java` e `GoalCreateDialog.java`.
3. **`GTD-03: Delegação canónica para o diálogo de criação`:**
   - `GoalsTab.java` delega para `GoalCreateDialog.show`.
4. **`GTD-04: Contratos e layout corporativo preservados`:**
   - Presença de `GoalsTab.BusinessUnitOption`, `UIHelper.ensureHeadersFit(table)`, `TableQuickPeekController.install(table` e `ModernPanel(16)`.

# HARNESS-EXECUTIVE-TV-DASHBOARD-001 — Matriz de Testes da Decomposição do Painel Executivo de Negócios (TV)

## 1. Escopo e Propósito

Este documento define os critérios de teste automatizado e auditoria estática para a decomposição do painel corporativo [ExecutiveTvDashboardDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/tv/ExecutiveTvDashboardDialog.java) e seus colaboradores extraídos em `mz.multicore.erp.gui.tv`.

## 2. Casos de Teste

1. **`testLineCountThresholds`**:
   - `ExecutiveTvDashboardDialog.java` deve ter $\le 200$ linhas.
   - `ExecutiveTvTheme.java` deve ter $\le 120$ linhas.
   - `ExecutiveTvCalculationHelper.java` deve ter $\le 100$ linhas.
   - `ExecutiveTvHeaderBuilder.java` deve ter $\le 200$ linhas.
   - `ExecutiveTvConsolidatedViewBuilder.java` deve ter $\le 190$ linhas.
   - `ExecutiveTvUnitViewBuilder.java` deve ter $\le 190$ linhas.
   - `ExecutiveTvTimerCoordinator.java` deve ter $\le 120$ linhas.

2. **`testZeroJOptionPaneUsage`**:
   - Garantir 0 ocorrências de `JOptionPane` em todos os arquivos de `mz.multicore.erp.gui.tv`.

3. **`testCalculationHelperCorrectness`**:
   - Validação da agregação de faturas por período e por unidade de negócio.
   - Validação de exclusão de unidades inativas da meta global.

4. **`testStatusBadgeAndKpiContract`**:
   - Garantir ausência de emojis nos badges de status (evita retângulos no Windows).
   - Validação das cores semânticas e consistência visual de temas Dark/Light.

# HARNESS-EXECUTIVE-TV-DASHBOARD-001 — Matriz de Testes da Decomposição do Painel Executivo de Negócios (TV)

## 1. Escopo e Propósito

Este documento define os critérios de teste automatizado e auditoria estática para a decomposição do painel corporativo [ExecutiveTvDashboardDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/tv/ExecutiveTvDashboardDialog.java) e seus colaboradores extraídos em `mz.multicore.erp.gui.tv`.

## 2. Casos de Teste

1. **`testLineCountThresholds`**:
   - `ExecutiveTvDashboardDialog.java` deve ter $\le 200$ linhas.
   - `ExecutiveTvTheme.java` deve ter $\le 120$ linhas.
   - `ExecutiveTvCalculationHelper.java` deve ter $\le 100$ linhas.
   - `ExecutiveTvHeaderBuilder.java` deve ter $\le 230$ linhas.
   - `ExecutiveTvConsolidatedViewBuilder.java` deve ter $\le 230$ linhas.
   - `ExecutiveTvUnitViewBuilder.java` deve ter $\le 225$ linhas.
   - `ExecutiveTvTimerCoordinator.java` deve ter $\le 120$ linhas.
   - Os valores acima são os de `ExecutiveTvDashboardDecompositionHarnessTest`; o teste é a referência.

2. **`testZeroJOptionPaneUsage`**:
   - Garantir 0 ocorrências de `JOptionPane` em todos os arquivos de `mz.multicore.erp.gui.tv`.

3. **`testCalculationHelperCorrectness`**:
   - Validação da agregação de faturas por período e por unidade de negócio.
   - Validação de exclusão de unidades inativas da meta global.

4. **`testStatusBadgeAndKpiContract`**:
   - Garantir ausência de emojis nos badges de status (evita retângulos no Windows).
   - Validação das cores semânticas e consistência visual de temas Dark/Light.

5. **`ExecutiveTvMetricsTest`** (redesenho de 2026-11-07):
   - Ritmo diário, previsão de fecho, % esperada e venda necessária por dia num mês de 30 dias.
   - Sem meta: estado `SEM META`, sem «ALERTA» nem «meta superada».
   - Limiares de `GoalStatus` iguais aos da especificação (50 / 80 / 100%).

## 3. Colaboradores do pacote `gui.tv` após o redesenho

| Classe | Responsabilidade |
|---|---|
| `ExecutiveTvTheme` | Tokens de cor (escuro/claro), crachás de estado |
| `ExecutiveTvComponents` | Cartão, KPI, crachá, botões, estado vazio, rolagem vertical |
| `GoalStatus` | Estado da meta (texto e cor) |
| `ExecutiveTvMetrics` | Indicadores puros, sem Swing |
| `ExecutiveTvSummaryBuilder` | Cartão principal e KPIs, partilhado pelas duas vistas |
| `ExecutiveTvListCards` | Top artigos e últimas faturas |
| `ExecutiveTvShareChart` | Anel de participação com legenda |

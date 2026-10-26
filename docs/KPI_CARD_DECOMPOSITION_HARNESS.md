# Documentação do Harness: Decomposição de KpiCard (Fase 92)

## 1. Objectivo

Documentar as garantias automatizadas fornecidas pelo teste de harness `KpiCardDecompositionHarnessTest` e pelos testes interligados (`KpiCardUniformityHarnessTest`, `DashboardCommercialKpiHarnessTest`, `ExecutiveUiExperienceHarnessTest`).

## 2. Casos de Teste

1. **KPID-01 (Limites Físicos Estritos):**
   - `KpiCard.java` $\le 250$ linhas (reduzido de 579 linhas).
   - `KpiCardPalette.java` $\le 180$ linhas.
   - `TrendBadge.java` $\le 115$ linhas.
   - `KpiCardInteractiveHelper.java` $\le 50$ linhas.
2. **KPID-02 (Zero Legado JOptionPane):**
   - Ausência absoluta de `JOptionPane.` em todos os ficheiros.
3. **KPID-03 (Delegação Canónica e Retrocompatibilidade):**
   - Valida existência de `KpiCard.TrendBadge` compatível com herança de `TrendBadge`.
   - Valida resolução de paletas via `KpiCardPalette`.
   - Valida interatividade via `KpiCardInteractiveHelper`.
4. **KPID-04 (Padrões Corporativos Preservados):**
   - `STANDARD_CARD_HEIGHT = 96`.
   - `STANDARD_CARD_MIN_WIDTH = 140`.
   - `createGrid`, `createCard`, `createMetricCard`, `createInteractiveCard`.

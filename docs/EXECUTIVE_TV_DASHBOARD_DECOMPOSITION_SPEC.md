# SPEC-EXECUTIVE-TV-DASHBOARD-DECOMPOSITION-001 — Decomposição Modular do Painel Executivo de Negócios (TV War Room)

## 1. Contexto e Diagnóstico

O painel de sala de reuniões e TV corporativa [ExecutiveTvDashboardDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/tv/ExecutiveTvDashboardDialog.java) acumulou **1.125 linhas**, concentrando num único ficheiro:
1. Gestão e alternância de temas (Dark Slate vs Executive Light).
2. Construção de cabeçalho corporativo com duas linhas, relógio em tempo real, botões de ação e filtros de período.
3. Coordenação de 3 timers concorrentes (relógio, auto-refresh de 45s e slideshow/rodízio rotativo de 15s).
4. Renderização do Master Card consolidado com super barra de progresso, 4 KPIs corporativos em linha, grid de unidades de distribuição, gráfico de pizza de share de faturamento e ranking dos Top Artigos campeões.
5. Renderização da visão focada em unidade de negócio específica, com KPIs dedicados, Top Artigos e grelha de últimas faturas emitidas.
6. Filtragem de dados por período e agregação de receitas e volumes.

Essa sobrecarga violava os limites físicos de manutenibilidade ($\le 180-220$ linhas para orquestradores e $\le 160-190$ linhas para colaboradores) e dificultava a testabilidade unitária dos cálculos executivos.

## 2. Objectivos da Refatoração

1. **Reduzir o orquestrador `ExecutiveTvDashboardDialog.java`** de 1.125 linhas para $\le 200$ linhas.
2. **Extrair colaboradores especializados no pacote `mz.multicore.erp.gui.tv`**:
   - `ExecutiveTvTheme`: Paleta dinâmica (Dark/Light), crachás de status sem emojis e cartões de KPI corporativos.
   - `ExecutiveTvHeaderBuilder`: Construção de cabeçalho em 2 linhas, botões de período, seletor de negócio e atalhos.
   - `ExecutiveTvCalculationHelper`: Filtragem temporal pura e cálculo de faturamento global e por unidade.
   - `ExecutiveTvConsolidatedViewBuilder`: Construção do master card global, grid de unidades e widgets da visão consolidada.
   - `ExecutiveTvUnitViewBuilder`: Construção da visão focada por unidade, top artigos e últimas faturas.
   - `ExecutiveTvTimerCoordinator`: Gerenciamento unificado dos timers (relógio, auto-refresh 45s e rodízio 15s).
3. **Preservar 100% dos contratos e compatibilidade**:
   - Construtor canónico `ExecutiveTvDashboardDialog(Window, ComercialApiClient, Long)`.
   - Enum público `ExecutiveTvDashboardDialog.Period`.
   - Atalhos de teclado [F11], [Espaço] e [ESC].
   - Integração perfeita com `BusinessUnitGoalManager` e `SellerPerformanceDialog`.
4. **Respeito irrestrito às directrizes do Multicore ERP**:
   - Zero chamadas a `JOptionPane`.
   - Zero caracteres Unicode especiais (sem emojis que viram retângulos no Windows).
   - Formatação monetária segura e uso estrito de `compareTo(BigDecimal.ZERO)` / `signum()`.
   - Textos visíveis estritamente em português de Moçambique (pt-MZ).

## 3. Critérios de Aceitação

- Orquestrador $\le 200$ linhas.
- Colaboradores individuais $\le 200$ linhas.
- Testes `ExecutiveTvDashboardDecompositionHarnessTest`, `DesktopThinContextTest` e `MultiModuleArchitectureHarnessTest` 100% aprovados.

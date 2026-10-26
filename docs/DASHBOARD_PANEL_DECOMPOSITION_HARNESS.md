# HARNESS-DASHBOARD-PANEL-DECOMPOSITION-001: Harness de Validação da Decomposição do DashboardPanel (Fase 12)

## 1. Objectivos do Teste de Harness
O teste automatizado `DashboardPanelDecompositionHarnessTest` verifica:
1. **DECOMP-01: Limites Físicos Estritos de Linhas de Código**:
   - `DashboardPanel.java` $\le 220$ linhas.
   - `DashboardDataCoordinator.java` $\le 200$ linhas.
   - `DashboardOverviewTab.java` $\le 240$ linhas.
   - `DashboardOperationsTab.java` $\le 150$ linhas.
2. **DECOMP-02: Zero Diálogos Legados `JOptionPane`**:
   - Nenhum dos ficheiros do domínio de dashboard pode invocar `JOptionPane.show...`.
3. **DECOMP-03: Integridade das Abas e Widgets**:
   - `DashboardPanel` expõe 3 abas corporativas:
     - "Visão Geral & Gráficos"
     - "Cockpit Operacional 360°"
     - "Rentabilidade & DRE"
   - Presença dos widgets `StrategicPulseWidget` e `OperationalPulseWidget`.
4. **DECOMP-04: Construtores Sobrecarregados e Navegação Interativa**:
   - Preservação dos construtores de 6, 11 e 12 argumentos.
   - Preservação da interatividade dos 8 cartões de KPI (HAND_CURSOR e tooltip).
5. **DECOMP-05: Ciclo de Vida e Inicialização Assíncrona Segura**:
   - Invocação de `refreshData()` sem NPE com clientes nulos ou mockados.

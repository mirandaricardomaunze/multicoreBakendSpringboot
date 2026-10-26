# SPEC-DASHBOARD-PANEL-DECOMPOSITION-001: Decomposição Modular do DashboardPanel (Fase 12)

## 1. Contexto & Diagnóstico
O `DashboardPanel.java` é actualmente o maior painel monolítico remanescente no sistema Multicore ERP, com **782 linhas de código**.
O painel concentra múltiplas responsabilidades de naturezas distintas:
1. **Orquestração de Dados & Cálculos**: Consulta assíncrona distribuída a 6 APIs (`ComercialApiClient`, `FinanceApiClient`, `ApprovalApiClient`, `CRMApiClient`, `PurchaseApiClient`, `InventoryApiClient`), cálculo de variações percentuais/tendências temporais, acumulação de vendas por produto (Top 5) e integração com o motor de DRE (`ProfitEngine`).
2. **Aba 1: Visão Geral & Gráficos**: Grelha de KPIs comerciais/tesouraria com badges de tendência, 4 gráficos em grelha 2x2 (`financialChart`, `salesChannelPieChart`, `operationsChart`, `financialStructurePieChart`), Top 5 produtos e feed de atividades recentes.
3. **Aba 2: Cockpit Operacional 360°**: Grelha de KPIs operacionais (aprovações, suporte, stock crítico, lotes a expirar), e integração dos widgets `StrategicPulseWidget` e `OperationalPulseWidget`.
4. **Aba 3: Rentabilidade & DRE**: Widget analítico de margens e rentabilidade (`ProfitAnalyticsWidget`).
5. **Barra de Períodos & Painel TV**: Chips de filtro temporal (Hoje, Semana, Mês, Ano, Todos) e acionamento de modo sala de reuniões (F11 / `ExecutiveTvDashboardDialog`).

## 2. Objectivos Arquitecturais
- **Limite Físico de Linhas:**
  - `DashboardPanel.java` $\le 220$ linhas (redução $\ge 72\%$).
  - `DashboardDataCoordinator.java` $\le 200$ linhas.
  - `DashboardOverviewTab.java` $\le 240$ linhas.
  - `DashboardOperationsTab.java` $\le 150$ linhas.
- **Zero Diálogos Legados:** Confirmar ausência absoluta de `JOptionPane`.
- **Preservação Integral de Contratos e APIs:**
  - Preservar os 3 construtores sobrecarregados de `DashboardPanel`:
    1. 6 parâmetros básicos de clientes API.
    2. 11 parâmetros incluindo auditoria forense, fluxo de caixa, risco de crédito, performance e handler de navegação.
    3. 12 parâmetros incluindo `DashboardApiClient`.
  - Preservar métodos públicos: `openExecutiveTvDashboard()`, `selectPeriod(PeriodFilter)`, `getCurrentPeriod()`, `updateWelcomeMessage(String, String)`, `refreshData()`, `getStrategicPulseWidget()`, `getOperationalPulseWidget()`.
  - Preservar o enum público `DashboardPanel.PeriodFilter`.
  - Compatibilidade total com `DashboardCommercialKpiHarnessTest`, `OperationalPulseWidgetHarnessTest`, `KpiDrilldownAndTableErgonomicsHarnessTest`, `ButtonContrastTest`, `ProductivitySuiteHarnessTest` e `DesktopThinContextTest`.

## 3. Matriz de Componentes e Responsabilidades

| Componente | Responsabilidade Principal | Limite Linhas |
|---|---|---|
| `DashboardPanel.java` | Orquestrador principal, inicialização de abas, barra de filtros temporais e atalho F11 | $\le 220$ |
| `DashboardDataCoordinator.java` | Consulta assíncrona às APIs, agregação de dados, cálculo de tendências, Top 5 e DRE | $\le 200$ |
| `DashboardOverviewTab.java` | Aba 1 (Visão Geral): KPIs comerciais, gráficos 2x2, Top Produtos e Atividades Recentes | $\le 240$ |
| `DashboardOperationsTab.java` | Aba 2 (Cockpit Operacional): KPIs de operações, stock crítico e lotes em risco | $\le 150$ |

# SPEC-HR-OVERVIEW-TAB-PANEL-DECOMPOSITION-001: Decomposição Modular da Visão Geral de Recursos Humanos

## 1. Visão Geral & Contexto Operacional

A aba `HROverviewTabPanel.java` (no módulo de Recursos Humanos do desktop) é responsável pela exibição de métricas estratégicas de RH, KPIs operacionais (colaboradores ativos, massa salarial mensal, INSS patronal, férias pendentes, faltas e notas de despesa) e gráficos estatísticos de evolução de folha e distribuição por departamento.

Actualmente com 196 linhas de código, a classe acumula a construção do layout de cartões de KPI e gráficos de barras, juntamente com toda a agregação analítica sobre múltiplas listas (`employeesList`, `payslipsList`, `vacationsList`, `absencesList`, `expensesList`), formatação monetária e montagem de dados para os gráficos.

Esta especificação define a extração das computações analíticas e atualização de métricas para um colaborador especializado:
- `HROverviewMetricsCoordinator.java`: Agregador analítico de estatísticas de folha de pagamento, férias, faltas, despesas e modelador de dados para os gráficos `SimpleBarChart`.
- `HROverviewTabPanel.java`: Orquestrador de visualização conciso focado na montagem da grelha de KPIs e painéis de gráficos.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `HROverviewTabPanel.java` | 196 | $\le 110$ | Orquestrador de visualização, grelha de 6 KPIs e integração de gráficos |
| `HROverviewMetricsCoordinator.java` | 0 (Novo) | $\le 115$ | Agregação estatística, recálculo analítico e povoamento dos gráficos |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - Package-private `HROverviewTabPanel(HRPanel owner)`.
   - Package-private `JPanel buildPanel()`.
   - Package-private `void refresh()`.
2. **Layout Canónico e UX:**
   - 6 KPIs canónicos: `"COLABORADORES ATIVOS"`, `"MASSA SALARIAL (MÊS)"`, `"INSS PATRONAL (MÊS)"`, `"FÉRIAS PENDENTES"`, `"FALTAS (MÊS)"`, `"DESPESAS PENDENTES"`.
   - 2 gráficos de barras (`SimpleBarChart`): `"Massa salarial líquida (6 meses)"` e `"Colaboradores por departamento"`.
3. **Segurança e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`.

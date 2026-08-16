# Spec — Visão Geral do RH (cards de KPI + gráficos)

> Nova aba **"Visão Geral"** no [HRPanel](../src/main/java/mz/multicore/erp/gui/HRPanel.java), com o mesmo
> aspecto profissional do [DashboardPanel](../src/main/java/mz/multicore/erp/gui/DashboardPanel.java).
> **Só apresentação** — sem mexer em `HRService`, DTOs ou regras. Alimentada por getters já
> existentes.

**Última actualização:** 2026-06-29

## Problema

O RH só tinha tabelas (Colaboradores, Recibos, Faltas, Férias, Despesas). Faltava uma **visão
executiva** — sem KPIs nem gráficos, o gestor não via de relance o estado da função RH (aspecto
amador face ao resto do ERP, que já tem dashboard com cards e gráficos).

## Decisões

- **Reutilizar (DRY) o padrão do Dashboard.** Os dois blocos visuais do Dashboard foram extraídos
  para componentes partilhados em `gui/components/`:
  - `KpiCard` — cartão de KPI (gradiente + ícone + valor + detalhe). `KpiCard.create(...)` e
    `KpiCard.valueLabel(...)`.
  - `SimpleBarChart` — gráfico de barras desenhado à mão (sem dependências externas).
  - O `DashboardPanel` passou a **delegar** nestes componentes (removida a classe interna duplicada).
- **Nova aba "Visão Geral"** como **primeira** aba do RH (ícone `fas-chart-pie`).
- **6 cards de KPI** (grelha 3×2), calculados em `refreshOverview()` a partir das listas já
  carregadas (`getAllEmployees/Payslips/Absences/Vacations/Expenses`):
  1. **Colaboradores ativos** (estado `ACTIVE`) — sub: total no quadro.
  2. **Massa salarial (mês)** — Σ líquido dos recibos do mês corrente (não cancelados); sub: bruto + nº recibos.
  3. **INSS patronal (mês)** — Σ `employerInss` do mês; sub: IRPS+INSS retido.
  4. **Férias pendentes** — recibos de férias `PENDING`; sub: dias por decidir.
  5. **Faltas (mês)** — faltas que se sobrepõem ao mês corrente; sub: dias não justificados.
  6. **Despesas pendentes** — notas de despesa `PENDING_APPROVAL`; sub: valor por aprovar.
- **2 gráficos:**
  - **Massa salarial líquida (6 meses)** — Σ líquido por mês (`YearMonth`), últimos 6 meses com recibos.
  - **Colaboradores por departamento** — headcount de activos por departamento (top 5).
- **Valores monetários** em formato pt-MZ (`%,.2f MT`); o gráfico usa o formato compacto (k/M) do
  `SimpleBarChart`.
- **Refresh** integrado: `refreshData()` chama `refreshOverview()`; a aba refresca ao seleccionar o
  módulo (`onPanelSelected`).

## Não-objetivos

- Não criar endpoints/serviços novos nem agregações no backend (tudo no cliente, sobre os getters).
- Não introduzir biblioteca de gráficos (JFreeChart, etc.).
- Não alterar as abas existentes além de acrescentar a nova como primeira.

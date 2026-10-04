# Especificação Técnica: Dashboard Comercial & Executivo — Variação de Vendas em Tempo Real, Ticket Médio e Margem Bruta

## 1. Visão Geral e Contexto de Negócio
Para operar como um ERP comercial e de retalho de classe empresarial (nível SAP Business One, Primavera, PHC), o Multicore ERP necessita de fornecer aos gestores e administradores visibilidade analítica em tempo real no **Dashboard Executivo**.
Anteriormente, o Dashboard exibia valores absolutos acumulados, mas carecia de:
1. **Indicador de Tendência Dinâmica (% de Variação):** Comparação das vendas do período selecionado em relação ao período imediatamente anterior (ex.: *Hoje vs Ontem*, *Este Mês vs Mês Anterior*).
2. **Ticket Médio Comercial:** Valor médio faturado por transação/venda comercial e POS.
3. **Margem Bruta Estimada (%):** Percentual de rentabilidade sobre as mercadorias vendidas no período.
4. **Badges de Tendência Modernas e Conformes:** Exibição com contraste adequado, cores semânticas vibrantes e ausência de caracteres não suportados pelo Windows Java 2D.

## 2. Requisitos Funcionais

### RF-01: Cálculo de Intervalos Temporais Comparativos
O motor `DashboardTrendCalculator` deve calcular os intervalos de datas atual e anterior com base no `PeriodFilter`:
- **HOJE:**
  - Período Atual: `[hoje, hoje]`
  - Período Anterior: `[ontem, ontem]` (`hoje.minusDays(1)`)
  - Rótulo de Comparação: `"vs ontem"`
- **ESTA_SEMANA:**
  - Período Atual: `[hoje - 7 dias, hoje]`
  - Período Anterior: `[hoje - 14 dias, hoje - 7 dias]`
  - Rótulo de Comparação: `"vs semana anterior"`
- **ESTE_MES:**
  - Período Atual: `[1º dia do mês corrente, hoje]`
  - Período Anterior: `[1º dia do mês anterior, fim do mês anterior]`
  - Rótulo de Comparação: `"vs mês anterior"`
- **ESTE_ANO:**
  - Período Atual: `[1º dia do ano corrente, hoje]`
  - Período Anterior: `[1º dia do ano anterior, fim do ano anterior]`
  - Rótulo de Comparação: `"vs ano anterior"`
- **TODOS:**
  - Período Atual: todo o histórico
  - Período Anterior: `null`
  - Rótulo de Comparação: `"histórico"`

### RF-02: Cálculo da Variação Percentual (Tendência)
A fórmula matemática da variação percentual é:
$$\Delta\% = \frac{V_{atual} - V_{anterior}}{V_{anterior}} \times 100$$
Regras de borda obrigatórias:
- Se $V_{anterior} == 0$ e $V_{atual} > 0 \implies +100.0\%$
- Se $V_{anterior} == 0$ e $V_{atual} == 0 \implies 0.0\%$
- Se $V_{anterior} > 0$ e $V_{atual} == 0 \implies -100.0\%$
- Em caso normal, cálculo com `BigDecimal.divide(..., 1, RoundingMode.HALF_UP)`.

### RF-03: Ticket Médio Comercial
$$TM = \frac{\text{Receita Total}}{\text{Nº Total de Vendas (Faturas + Vendas POS)}}$$
Se não houver transações ($N = 0$), o Ticket Médio é `0.00 MT`.
Formatação no ecrã: `"X.XX MT/venda"`.

### RF-04: Margem Bruta Comercial Estimada (%)
A margem percentual consolidada vem de:
$$\text{Margem\%} = \frac{\text{Lucro Bruto}}{\text{Receita Total}} \times 100$$
Exibida no card executivo em destaque com cores temáticas.

### RF-05: Integração com UI Swing no Dashboard
- O card de **FATURAÇÃO TOTAL** exibe o valor faturado e o `TrendBadge` (com ícones vetoriais FontAwesome `fas-arrow-up` / `fas-arrow-down` / `fas-minus`, proibidos caracteres Unicode crus como `▲` ou `▼`) com o subtítulo do período de comparação.
- O card de **VENDAS POS** exibe o total arrecadado no POS, a contagem de recibos e a respectiva tendência com badge de variação e ticket médio.
- A grelha de KPIs do topo passa a acomodar métricas comerciais executivas vitais sem comprometer a densidade visual.

## 3. Requisitos Não Funcionais e Arquiteturais
- **Limite de Linhas:** `DashboardPanel.java` deve permanecer estritamente abaixo de 1000 linhas ($\le 1000$). Toda a matemática de tendência é isolada em `DashboardTrendCalculator.java`.
- **EDT Safety:** Nenhuma consulta de rede ou processamento de lista pode bloquear a Event Dispatch Thread do Swing; todo o carregamento assíncrono ocorre via `UIHelper.loadAsync`.
- **Independência de Módulos:** `DashboardTrendCalculator` reside no módulo `desktop` e opera sobre DTOs do `contracts` (`InvoiceDTO`, `POSSalesSummaryDTO`, etc.), sem dependência de JPA ou backend.

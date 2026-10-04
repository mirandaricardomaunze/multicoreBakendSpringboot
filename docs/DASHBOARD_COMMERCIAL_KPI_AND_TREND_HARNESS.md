# Matriz de Testes (Harness): Dashboard Comercial & Executivo — Variação de Vendas, Ticket Médio e Margem Bruta

## 1. Objectivo
Garantir que a inteligência de negócios do Dashboard Executivo, o cálculo determinístico de períodos comparativos, a variação percentual com tratamento de borda (divisão por zero, nulos), o ticket médio por transação e a renderização fluida sem caracteres Unicode proibidos funcionam com 100% de precisão e sem regressões na interface Swing.

---

## 2. Casos de Teste Automatizados

| ID | Cenário | Entrada | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **DKPI-01** | Intervalos comparativos para cada `PeriodFilter` | `HOJE`, `ESTA_SEMANA`, `ESTE_MES`, `ESTE_ANO`, `TODOS` com data-base fixa | Período anterior calculado rigorosamente (ontem, semana anterior, mês anterior, ano anterior); `TODOS` com anterior nulo. |
| **DKPI-02** | Variação percentual normal (crescimento e decrescimento) | Atual: `1500.00 MT`, Anterior: `1000.00 MT`<br>Atual: `800.00 MT`, Anterior: `1000.00 MT` | `+50.0%`<br>`-20.0%` |
| **DKPI-03** | Casos de borda da variação percentual | Atual: `500.00`, Anterior: `0.00`<br>Atual: `0.00`, Anterior: `500.00`<br>Atual: `0.00`, Anterior: `0.00` | `+100.0%`<br>`-100.0%`<br>`0.0%` |
| **DKPI-04** | Cálculo do Ticket Médio Comercial | Receita: `15000.00 MT`, 10 vendas<br>Receita: `0.00 MT`, 0 vendas | `1500.00 MT/venda`<br>`0.00 MT/venda` |
| **DKPI-05** | Cálculo da Margem Bruta Estimada (%) | Receita: `20000.00 MT`, Lucro: `5000.00 MT`<br>Receita: `0.00 MT`, Lucro: `0.00 MT` | `25.0%`<br>`0.0%` |
| **DKPI-06** | Componente visual `TrendBadge` sem emojis/Unicode proibidos | Instanciação e atualização com percentual positivo, negativo e neutro | Sem `▲`, `▼` ou `—` crus de sistema; usa ícones FontAwesome (`fas-arrow-up`, `fas-arrow-down`, `fas-minus`); cores semânticas de alto contraste. |
| **DKPI-07** | Resiliência contra dados nulos e clientes desacoplados | Chamadas com DTOs parciais, listas vazias ou valores nulos | Retornos graciosos com zero e sem `NullPointerException`. |
| **DKPI-08** | Decomposição de Linhas e Arquitetura | Verificação de ficheiros | `DashboardPanel.java <= 1000 linhas`, `DashboardTrendCalculator.java <= 1000 linhas`. |

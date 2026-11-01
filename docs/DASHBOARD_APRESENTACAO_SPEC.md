# Apresentação do Painel Inicial — Especificação e harness

**Última actualização:** 2026-11-01

## Objectivo

O painel deve ser útil logo ao abrir, mesmo antes da primeira venda do dia. Não altera os cálculos
financeiros nem a origem dos dados.

## Decisões

- **Período sem movimento:** o texto dos cartões de Faturação e Vendas POS passa a mostrar o período
  anterior, por exemplo `Ontem: 1.500,50 MT`.
- **Pendências primeiro:** a primeira linha de gráficos tem "Operações e Pendências" e "Vendas,
  Compras e IVA".
- **Gráficos vazios:** sem valores positivos, o gráfico de barras mostra uma mensagem centrada, sem
  barras a zero. Em "Operações e Pendências", a mensagem é "Sem pendências".
- **Gráficos financeiros:** os circulares só aparecem se houver vendas ou compras no período.
- **Cartão POS:** conta as vendas do período escolhido. Antes, mostrava sempre as de hoje.
- **Painel TV:** usa a empresa activa. Antes, abria sempre a empresa 1. É um botão só com ícone,
  separado dos filtros de período, porque é uma acção e não um filtro.
- **Barra de estado:** deixa de repetir o módulo e a empresa. O total de registos nunca era
  preenchido, por isso saiu também. Ficam o produto, o aviso de versão e a hora.
- **Formato:** percentagens com vírgula (`+8,5%`, `0,0%`).
- **Textos:**
  - "Cockpit Operacional 360°" passa a "Operações".
  - "Rentabilidade & DRE" passa a "Rentabilidade & Resultados".
  - "Ticket Médio" e "TM" passam a "Valor médio", também em Ranking, Desempenho de Vendedores e no
    relatório PDF de desempenho.
  - "Liq" e "Ded" passam a "Liquidado" e "Dedutível".

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| DA-01 | Sem vendas no período | Valor do período anterior no cartão |
| DA-02 | Com vendas, ou "Todo o Período" | Texto normal do cartão |
| DA-03 | Todos os valores a zero ou nulos | Gráfico considerado vazio |
| DA-04 | Com e sem movimento | Pendências primeiro; gráficos circulares só com dados |
| DA-05 | Cartão POS e percentagens | Contagem do período e vírgula decimal |
| DA-06 | Painel TV | Só ícone, antes dos períodos e separado deles |
| DA-07 | Barra de estado | Só produto e hora, sem módulo nem empresa |

Teste: `DashboardPresentationHarnessTest`.

## Limites

- As quatro cores fortes dos cartões mantêm-se: `.agents/rules/swing_ui_rules.md`, §5, exige uma cor
  própria por métrica. Mudar isto obriga a rever primeiro essa regra.
- As setas de deslocação mantêm-se: são decisão de `DASHBOARD_ARROW_SCROLL_OVERFLOW_SPEC.md`.
- O nome do utilizador continua a aparecer na saudação, no topo e na barra lateral: os dois últimos
  têm o botão de sair exigido pelas regras de UI.

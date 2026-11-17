# Apresentação do Painel Inicial — Especificação e harness

**Última actualização:** 2026-11-16

## Objectivo

O painel deve ser útil logo ao abrir, mesmo antes da primeira venda do dia. Não altera os cálculos
financeiros nem a origem dos dados.

## Decisões

- **Período sem movimento:** o texto dos cartões de Faturação e Vendas POS passa a mostrar o período
  anterior, por exemplo `Ontem: 1.500,50 MT`.
- **Pendências primeiro:** a primeira linha de gráficos tem "Operações e Pendências" e "Vendas,
  Compras e IVA".
- **Gráficos vazios:** sem valores positivos, o gráfico de barras mostra uma mensagem centrada, sem
  barras a zero.
- **Operações e Pendências (2026-11-16):** lista, não gráfico. Três contagens de coisas diferentes
  desenhadas como barras pareciam enormes com valor 1. Cada linha (Aprovações, Tickets abertos,
  Stock baixo) mostra ícone, descrição, contagem e seta, e abre o ecrã que resolve o assunto. A zero,
  a linha fica cinzenta com "Nada por tratar"; tudo a zero mostra o visto verde e "Tudo em dia".
- **Período inicial (2026-11-16):** o painel abre em "Este Mês". Em "Hoje", antes da primeira venda,
  metade do ecrã ficava "Sem dados".
- **Barras a zero:** com outros valores positivos, a barra a zero não se pinta; fica só o "0" na base.
  Um risco colorido parecia um valor pequeno ou um erro.
- **Circular com uma só fatia:** em vez de um disco inteiro de uma cor (100% / 0%), mostra a quota em
  grande, o valor, uma barra cheia e as outras categorias a 0%. Vale para todos os `SimplePieChart`.
- **Gráficos financeiros:** os circulares só aparecem se houver vendas ou compras no período.
- **Cartão POS:** conta as vendas do período escolhido. Antes, mostrava sempre as de hoje.
- **Painel TV:** usa a empresa activa. Antes, abria sempre a empresa 1. É o botão "Painel Executivo",
  com ícone e nome, em estilo neutro, separado dos filtros de período porque é uma acção e não um
  filtro. Antes era só um ícone num azul diferente do resto.
- **Períodos:** num só controlo segmentado (`DashboardPeriodBar`); só o activo tem cor, num tom suave
  de azul com texto azul (desde 2026-11-16), para não competir com o título.
- **Atividade recente:** o estado da fatura sai em português ("Aprovado"), nunca o enum em inglês.
- **Títulos de cartão:** sem ícone, iguais em todos os cartões do painel.
- **Empresa no topo:** a caixa fechada mostra só o nome; o papel aparece na lista aberta.
- **Cartões:** o valor começa no mesmo sítio, na horizontal e na vertical, com ou sem pílula de variação. Em "Todo o Período" a
  pílula esconde-se, porque não há período anterior. O cartão do IVA chama-se "IVA do período" e
  mostra só o montante; "A pagar" ou "A recuperar" vai para a legenda. O saldo de tesouraria diz na
  legenda que é o saldo actual, porque não depende do período.
- **Nome da empresa no menu:** parte em duas linhas antes do botão de recolher.
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
| DA-04 | Com e sem movimento | Lista de pendências primeiro, "Tudo em dia" a zero; circulares só com dados |
| DA-05 | Cartão POS e percentagens | Contagem do período e vírgula decimal |
| DA-06 | Painel TV | "Painel Executivo", antes dos períodos e separado deles |
| DA-07 | Barra de estado | Só produto e hora, sem módulo nem empresa |
| DA-08 | Circular com uma só fatia | Quota em barra, sem disco |
| DA-09 | Barra com valor zero | Não se pinta, mas mostra o valor ao passar o rato |
| DA-10 | "Todo o Período" | Pílulas de variação escondidas |
| DA-11 | Cartão do IVA | Só o montante no valor; sentido na legenda |
| DA-12 | Períodos | Um só controlo, cor só no activo |
| DA-13 | Cartão com pílula | Valor sem recuo |

Testes: `DashboardPresentationHarnessTest` (DA-01 a DA-07) e `DashboardReadabilityHarnessTest`
(DA-08 a DA-13).

## Limites

- As quatro cores fortes dos cartões mantêm-se: `.agents/rules/swing_ui_rules.md`, §5, exige uma cor
  própria por métrica. Mudar isto obriga a rever primeiro essa regra.
- As setas de deslocação saíram em 2026-11-16 a pedido do responsável ("design bonito"): ficou uma
  barra de scroll fina, só quando é precisa (ver `DASHBOARD_ARROW_SCROLL_OVERFLOW_SPEC.md`).
- Menu lateral (2026-11-16): ícones neutros em repouso; a cor do módulo só no item activo ou com o
  rato por cima.
- O nome do utilizador continua a aparecer na saudação, no topo e na barra lateral: os dois últimos
  têm o botão de sair exigido pelas regras de UI.

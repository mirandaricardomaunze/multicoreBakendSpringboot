# Faixa compacta de KPIs nos ecrãs com tabela

**Código:** SPEC-KPI-FAIXA  
**Módulo:** `desktop` (`mz.multicore.erp.gui.components`)  
**Data:** 2026-11-13  
**Estado:** APROVADO / IMPLEMENTADO  
**Harness:** `desktop/src/test/java/mz/multicore/erp/gui/components/KpiStripHarnessTest.java`

## 1. Problema

Os cartões KPI têm 96 px de altura. Por cima de uma tabela, com espaçamentos, ocupavam 120 a 130 px:
num ecrã 1366×768 isso era cerca de um quarto da área útil, e a tabela ficava com 8 a 10 linhas visíveis.

## 2. Regra

| Tipo de ecrã | O que o indicador é | Grelha | Altura do cartão |
|---|---|---|---|
| Operacional (lista, extrato, aprovação, stock…) | Contexto da tabela | `KpiCard.createGrid(...)` | 56 px (faixa compacta) |
| Painel de indicadores (dashboard, ficha de artigo) | O próprio conteúdo | `KpiCard.createHeroGrid(...)` | 96 px |

- **Uma só regra, um só sítio (DRY):** `KpiCard.createGrid` devolve uma `KpiStrip`. Cada cartão
  criado por `KpiCard.create/createCard/createMetricCard` e adicionado à faixa é redesenhado pelo
  `KpiCompactLayout`. Os ecrãs não mudam: continuam a criar cartões e a fazer `setText` nos mesmos `JLabel`.
- **Cartão compacto:** ícone de 16 px à esquerda na cor de acento, título em maiúsculas (10 px),
  valor em negrito (17 px) com a nota na mesma linha. A nota encolhe e, se mesmo assim não couber,
  termina em reticências com o texto completo no tooltip. Mantém a faixa de acento, a paleta, o
  ajuste de letra do valor e o clique de drilldown (`makeInteractive`).
- **Painéis de indicadores** (lista fechada no harness): `DashboardOverviewTab`,
  `DashboardOperationsTab`, `SellerDashboardViewCoordinator`, `StockProductDetailHeroBuilder`,
  `StrategicPulseWidget`, `OperationalPulseWidget`, `ProfitAnalyticsWidget`. Um ecrã novo só entra
  nesta lista se o indicador for o conteúdo principal e não houver tabela por baixo.
- Fora de âmbito: POS (`PosTodaySummaryView`, design próprio em `POS_DESIGN_ECRA_SPEC.md`) e a
  visão geral de RH (só cartões e gráficos, sem tabela).

## 3. Critérios de aceitação

| ID | Critério | Teste |
|---|---|---|
| KFC-01 | `createGrid` encolhe cada cartão adicionado para 56 px (≤ 60% dos 96 px) | `kfc01_gridCompactsCards` |
| KFC-02 | Os `JLabel` de valor e nota continuam a ser os do ecrã; nota na linha do valor | `kfc02_labelsStayLive` |
| KFC-03 | `createHeroGrid` mantém 96 px | `kfc03_heroKeepsFullHeight` |
| KFC-04 | Cartão compacto continua clicável (cursor de mão e acção) | `kfc04_compactCardKeepsDrilldown` |
| KFC-05 | Só os painéis da lista usam `createHeroGrid` | `kfc05_heroGridOnlyOnDashboards` |
| KFC-06 | Nenhum ecrã com `JTable` monta KPIs numa grelha improvisada | `kfc06_noAdHocKpiGridsAboveTables` |
| KFC-07 | Valor grande (ex. `1.125.450.000.000,00 MT`) nunca passa da borda, de 150 a 400 px: a letra encolhe, depois a nota esconde-se, por fim reticências com tooltip | `kfc07_largeValueNeverOverflows` |

## 4. Próximos passos possíveis

- Contagens por estado como chips de filtro activos (o cartão clicado fica marcado e filtra a tabela).
- Somas monetárias numa linha de totais fixa no rodapé da tabela.

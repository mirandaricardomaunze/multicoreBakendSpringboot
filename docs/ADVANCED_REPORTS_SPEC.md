# Especificação — Relatórios Gerenciais Avançados com Exportação Profissional

**Versão:** 1.0  
**Data:** 2026-09-14  
**Domínio dono:** `reports`  
**Harness:** `AdvancedReportsHarnessTest`

---

## 1. Contexto e o que já existe

O módulo `reports` actual tem:

| O que existe | Onde |
|---|---|
| `ReportService.buildStoreDashboard()` | Dashboard com KPIs do dia |
| `ReportService.buildDailyStoreReport()` | Relatório diário com margens, operadores, meios de pagamento |
| `ProductMarginDTO` | Margem bruta por produto (receita − custo histórico) |
| `OperatorSalesSummaryDTO` | Vendas por operador (quem emitiu a fatura) |
| `TopProductDTO` | Top 5 produtos por quantidade vendida |
| `LowStockAlertDTO` | Alertas de stock mínimo |
| `TableExportPrintService` + `TablePdfExporter` | Exportação de qualquer tabela do ecrã para PDF com cabeçalho da empresa |

**O que falta** para uma área de relatórios executiva:
- Filtros por **período arbitrário** (não só "hoje").
- Filtros por **armazém** e **categoria**.
- **Curva ABC** de produtos (classificação por contribuição cumulativa de receita).
- **Produtos parados** (sem venda num período).
- **Produtos com baixa margem** (margem % < limiar configurável).
- **Ruptura de stock e previsão de reposição** (dias de stock restantes).
- **Vendas por trabalhador/caixa/terminal** com mais detalhe (total, nº transações, ticket médio).
- **Comparativo por período** (hoje vs 7 dias vs 30 dias vs mês actual) dentro do mesmo relatório.
- **Exportação CSV** (além de PDF já existente).
- **Painel dedicado de Relatórios** na UI desktop.

---

## 2. Os oito relatórios a implementar

### REP-01 — Margem por Produto / Categoria / Armazém

**Pergunta:** Onde está a margem? Qual categoria mais rende?

| Campo de saída | Origem |
|---|---|
| SKU, Nome, Categoria | `InvoiceLine → Product → category.name` |
| Armazém | `Invoice.warehouse.name` |
| Receita | `sum(InvoiceLine.lineTotal)` |
| Custo histórico | `sum(InvoiceLine.lineCost())` |
| Margem bruta (MZN) | receita − custo |
| Margem bruta (%) | (margem / receita) × 100 |

**Filtros:** período (from/to), armazémId, categoriaId.  
**Agrupamento:** por produto; totais de categoria e armazém separados em sub-relatórios.

---

### REP-02 — Curva ABC de Produtos

**Pergunta:** Quais os 20% de produtos que geram 80% da receita?

Algoritmo:
1. Ordenar produtos por receita acumulada no período (descendente).
2. Calcular % cumulativa sobre receita total.
3. Classificar:
   - **A** — 0–70% cumulativo (poucos produtos, alta contribuição)
   - **B** — 70–90% cumulativo
   - **C** — 90–100% cumulativo (muitos produtos, baixa contribuição)

| Campo de saída | Detalhe |
|---|---|
| SKU, Nome, Categoria | — |
| Receita no período | — |
| % sobre total | — |
| % cumulativa | — |
| Classe ABC | A / B / C |

**Filtros:** período, categoriaId.

---

### REP-03 — Produtos Parados

**Pergunta:** O que está no stock sem vender?

Lógica: produtos com `Stock.quantity > 0` para a empresa e **sem** nenhuma `InvoiceLine` num período (ex.: últimos 30 dias).

| Campo | Detalhe |
|---|---|
| SKU, Nome, Categoria | — |
| Armazém | — |
| Stock actual | `Stock.quantity` |
| Dias sem venda | dias desde a última `InvoiceLine` (ou `null` se nunca vendido) |
| Valor em stock (MZN) | `Stock.quantity × Product.purchasePrice` |

**Filtros:** período de análise, armazémId.

---

### REP-04 — Produtos com Baixa Margem

**Pergunta:** O que estou a vender sem lucro?

Produtos com `margemPct < limiarPct` (configurável na chamada, default 10%).

Campos = REP-01 + `limiarPct` recebido no filtro.

---

### REP-05 — Ruptura de Stock e Previsão de Reposição

**Pergunta:** O que vai faltar? Quando devo encomendas?

Lógica:
- **Velocidade de saída** = `qtd vendida no período / nº dias do período` (média diária de saídas)
- **Dias de stock restantes** = `stockActual / velocidadeSaída` (∞ se vendas = 0)
- **Em ruptura** = `stockActual ≤ 0`
- **Em risco** = `diasRestantes ≤ 7`

| Campo | Detalhe |
|---|---|
| SKU, Nome | — |
| Armazém | — |
| Stock actual | — |
| Velocidade (unid/dia) | — |
| Dias de stock restantes | — |
| Data estimada de ruptura | hoje + diasRestantes |
| Estado | EM_RUPTURA / EM_RISCO / NORMAL |

**Filtros:** armazémId; período para calcular a velocidade.

---

### REP-06 — Vendas por Trabalhador / Caixa / Terminal

**Pergunta:** Quem vende mais? Qual terminal tem mais movimento?

Agrupa `Invoice` por:
- `createdBy` (operador/trabalhador)
- `salesChannel` (COUNTER, POS, ONLINE…)
- `warehouse.name` (armazém/loja)

| Campo | Detalhe |
|---|---|
| Operador / Terminal / Armazém | chave de agrupamento |
| Nº de vendas | `count(Invoice)` |
| Receita total | `sum(totalAmount)` |
| Ticket médio | `receita / nrVendas` |
| Período | from/to |

**Filtros:** período, operador, armazémId, salesChannel.

---

### REP-07 — Comparativo por Período

**Pergunta:** Hoje está melhor ou pior do que a semana passada?

Um único endpoint que devolve **4 períodos lado a lado**:

| Período | Definição |
|---|---|
| Hoje | `[hoje 00:00, agora]` |
| Últimos 7 dias | `[hoje−7, hoje]` |
| Últimos 30 dias | `[hoje−30, hoje]` |
| Mês actual | `[1º do mês, hoje]` |

Por período: `{ totalVendas, nrFaturas, margemBruta, ticketMedio }`.

Campo `variacao` = `(periodoActual − periodoAnterior) / periodoAnterior × 100`.

---

### REP-08 — Exportação CSV

**O que já existe:** `TableExportPrintService` aceita qualquer tabela → PDF.  
**O que falta:** endpoint que converta o mesmo `TableExportRequest` → CSV UTF-8.

Formato: RFC 4180, separador `,`, codificação UTF-8 com BOM (compatível com Excel).  
Cabeçalho com nome da empresa e data de emissão nas primeiras linhas como comentário.

---

## 3. Contratos REST novos

Todos em `/api/reports/`:

| Endpoint | Método | Parâmetros | Resposta |
|---|---|---|---|
| `/api/reports/margin` | GET | `companyId, from, to, warehouseId?, categoryId?` | `List<ProductMarginRowDTO>` |
| `/api/reports/abc` | GET | `companyId, from, to, categoryId?` | `AbcReportDTO` |
| `/api/reports/slow-movers` | GET | `companyId, from, to, warehouseId?` | `List<SlowMoverDTO>` |
| `/api/reports/low-margin` | GET | `companyId, from, to, marginThresholdPct?` | `List<ProductMarginRowDTO>` |
| `/api/reports/stock-rupture` | GET | `companyId, warehouseId?, velocityDays?` | `List<StockRuptureDTO>` |
| `/api/reports/sales-by-operator` | GET | `companyId, from, to, warehouseId?` | `List<OperatorDetailDTO>` |
| `/api/reports/period-comparison` | GET | `companyId` | `PeriodComparisonDTO` |
| `/api/print/csv` | POST | `companyId` + `TableExportRequest` body | `text/csv` |

---

## 4. Novos DTOs (todos em `contracts/src/main/java/mz/multicore/erp/modules/reports/dto/`)

| DTO | Campos chave |
|---|---|
| `ProductMarginRowDTO` | productId, sku, name, categoryName, warehouseName, revenue, cost, grossMargin, marginPct |
| `AbcReportDTO` | `List<AbcRowDTO>` items + `BigDecimal totalRevenue` |
| `AbcRowDTO` | sku, name, revenue, revenuePct, cumulativePct, abcClass |
| `SlowMoverDTO` | productId, sku, name, categoryName, warehouseName, currentStock, daysSinceLastSale, stockValueMzn |
| `StockRuptureDTO` | productId, sku, name, warehouseName, currentStock, avgDailySales, daysOfStockLeft, estimatedRuptureDate, status (enum: RUPTURED/AT_RISK/NORMAL) |
| `OperatorDetailDTO` | operator, salesChannel, warehouseName, salesCount, totalRevenue, avgTicket |
| `PeriodComparisonDTO` | `PeriodSummaryDTO today, last7, last30, currentMonth` |
| `PeriodSummaryDTO` | label, from, to, totalRevenue, invoiceCount, grossMargin, avgTicket, variationPct |
| `ReportFilterDTO` | companyId, from, to, warehouseId, categoryId, marginThresholdPct, velocityDays |

---

## 5. Painel de Relatórios no Desktop

### `AdvancedReportsPanel.java` — novo painel principal

Estrutura:
```
[Barra de Filtros — sempre visível: período, armazém, categoria]
[Abas: Margem | Curva ABC | Parados | Baixa Margem | Ruptura | Operadores | Comparativo]
[Cada aba:
   KPI bar (3–4 cards) no topo
   Tabela de resultados (filterable com TableFilter)
   Barra de acções: Exportar PDF | Exportar CSV
]
```

### Integração no `MainFrame`

Adicionar `AdvancedReportsPanel` à sidebar como item **"Relatórios"** (ícone `fas-chart-bar`),  
visível para `MANAGER` e `ADMIN`.

---

## 6. Regras de negócio críticas

| ID | Regra |
|---|---|
| REP-BIZ-01 | Custo histórico (`lineCost()`) — nunca o preço de compra actual do produto. |
| REP-BIZ-02 | Margem % só é calculada quando `revenue > 0`; caso contrário `null` (sem divisão por zero). |
| REP-BIZ-03 | Velocidade de saída (REP-05) usa apenas faturas com `isRealisedSale() = true`. |
| REP-BIZ-04 | Curva ABC usa apenas receita de vendas realizadas no período (mesma regra). |
| REP-BIZ-05 | Produtos parados: um produto com `stockTracked = false` é excluído do REP-03 e REP-05. |
| REP-BIZ-06 | CSV: caracteres `,` e `"` em valores são escapados per RFC 4180. |
| REP-BIZ-07 | Todos os relatórios verificam `CurrentUserContext.requireCompany(companyId)`. |
| REP-BIZ-08 | Permissão de leitura: `MANAGER` ou `ADMIN`. `EMPLOYEE` não acede a relatórios gerenciais. |
| REP-BIZ-09 | Intervalo `from`/`to` máximo: 366 dias. Acima → `BusinessRuleException`. |
| REP-BIZ-10 | Comparativo (REP-07) não aceita `from`/`to` externos — os períodos são calculados internamente. |

---

## 7. Estrutura de ficheiros

### contracts/
- `[NEW]` `reports/dto/ProductMarginRowDTO.java`
- `[NEW]` `reports/dto/AbcReportDTO.java`, `AbcRowDTO.java`
- `[NEW]` `reports/dto/SlowMoverDTO.java`
- `[NEW]` `reports/dto/StockRuptureDTO.java`
- `[NEW]` `reports/dto/OperatorDetailDTO.java`
- `[NEW]` `reports/dto/PeriodComparisonDTO.java`, `PeriodSummaryDTO.java`

### backend/
- `[NEW]` `reports/service/MarginReportService.java` — REP-01, REP-04
- `[NEW]` `reports/service/AbcReportService.java` — REP-02
- `[NEW]` `reports/service/SlowMoverReportService.java` — REP-03
- `[NEW]` `reports/service/StockRuptureReportService.java` — REP-05
- `[NEW]` `reports/service/OperatorSalesReportService.java` — REP-06
- `[NEW]` `reports/service/PeriodComparisonReportService.java` — REP-07
- `[MODIFY]` `reports/controller/ReportController.java` — adicionar 7 novos endpoints
- `[NEW]` `printing/CsvExportService.java` — REP-08
- `[MODIFY]` `printing/PrintController.java` — novo endpoint `/api/print/csv`
- `[NEW]` `reports/repository/InvoiceLineReportRepository.java` — queries JPQL de agregação

### desktop/
- `[NEW]` `desktop/client/ReportApiClient.java` — cliente HTTP para todos os endpoints de relatório
- `[NEW]` `gui/AdvancedReportsPanel.java` — painel principal com abas
- `[NEW]` `gui/reports/MarginReportTab.java`
- `[NEW]` `gui/reports/AbcReportTab.java`
- `[NEW]` `gui/reports/SlowMoverReportTab.java`
- `[NEW]` `gui/reports/LowMarginReportTab.java`
- `[NEW]` `gui/reports/StockRuptureReportTab.java`
- `[NEW]` `gui/reports/OperatorSalesReportTab.java`
- `[NEW]` `gui/reports/PeriodComparisonReportTab.java`
- `[MODIFY]` `gui/MainFrame.java` — adicionar AdvancedReportsPanel à sidebar

### testes/
- `[NEW]` `reports/service/AdvancedReportsHarnessTest.java` — harness REP-BIZ-01 a REP-BIZ-10

---

## 8. Critérios de aceitação

1. **REP-01**: Admin filtra por "Bebidas" + "Armazém Central" + "Setembro 2026" → vê margem por produto com `%`.
2. **REP-02**: Admin selecciona último mês → tabela com classes A/B/C, linha de total, % cumulativa.
3. **REP-03**: Lista produtos com stock > 0 sem venda nos últimos 30 dias, com valor em stock.
4. **REP-04**: Filtra `margem < 5%` → lista ordenada do pior para o melhor margem.
5. **REP-05**: Produtos com `diasRestantes ≤ 7` aparecem a vermelho; `EM_RUPTURA` aparecem em destaque.
6. **REP-06**: Filtra por operador "joao" → vê nº vendas, total, ticket médio.
7. **REP-07**: 4 colunas — Hoje / 7 dias / 30 dias / Mês — com variação `+12%` a verde e `-5%` a vermelho.
8. **Exportar PDF**: clica "Exportar PDF" → abre diálogo para guardar ficheiro; PDF tem cabeçalho da empresa.
9. **Exportar CSV**: clica "Exportar CSV" → ficheiro `.csv` abrível no Excel com todos os dados filtrados.
10. `EMPLOYEE` logado → menu "Relatórios" não aparece (guarda de permissão).

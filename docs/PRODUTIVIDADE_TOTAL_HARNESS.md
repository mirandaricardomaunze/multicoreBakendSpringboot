# Harness de Testes — Pacote de Produtividade, Gestão e Rigor Operacional

Este documento define os critérios de conformidade e testes automatizados para validar as 5 funcionalidades especificadas em `PRODUTIVIDADE_TOTAL_SPEC.md`.

---

## Critérios de Conformidade e Casos de Teste

| ID | Requisito | Critério de Aceitação | Teste Automatizado |
|---|---|---|---|
| **PT-01** | Exportação CSV de Tabelas | `TableCsvExporter` gera CSV válido em UTF-8 com escape de aspas, separador `;` e respeitando colunas visíveis. | `ProductivitySuiteHarnessTest.pt01_exportsTableToCsvWithProperEscapingAndColumns` |
| **PT-02** | Menu de Contexto com Exportação | `TableContextMenu` contém a opção "Exportar para CSV (Ctrl+E)" e atalho associado. | `ProductivitySuiteHarnessTest.pt02_tableContextMenuIncludesCsvExportOption` |
| **PT-03** | Fecho Cego de Caixa no POS | `PosCashSessionActions` processa o fecho cego sem expor o saldo esperado previamente. | `ProductivitySuiteHarnessTest.pt03_posBlindCashCloseCalculatesDiscrepancy` |
| **PT-04** | Gerador de Etiquetas de Stock | `ShelfLabelsDialog` formata e renderiza etiquetas com nome, código/barcode e preço em MT. | `ProductivitySuiteHarnessTest.pt04_shelfLabelsDialogRendersFormattedProductLabels` |
| **PT-05** | Ranking Top 5 Produtos | O componente de Top Produtos agrega e classifica os itens por valor de venda de forma decrescente. | `ProductivitySuiteHarnessTest.pt05_topProductsAggregationOrdersByRevenue` |
| **PT-06** | Feed de Atividade Recente | `RecentActivityWidget` renderiza eventos recentes com ícones, títulos e timestamps formatados. | `ProductivitySuiteHarnessTest.pt06_recentActivityWidgetRendersTimelineEntries` |
| **PT-07** | Filtros de Período no Dashboard | `DashboardPanel` suporta comutação entre períodos (Hoje, Esta Semana, Este Mês, Este Ano, Todos) sem exceção. | `ProductivitySuiteHarnessTest.pt07_dashboardPeriodFilteringAdaptsData` |

---

## Execução

```bash
mvn test -pl desktop "-Dtest=ProductivitySuiteHarnessTest"
```

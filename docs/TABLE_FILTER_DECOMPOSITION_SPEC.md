# Especificação de Decomposição: TableFilter (Fase 198)

## Objectivo
Decompor o utilitário de filtragem tabular `TableFilter.java` (439 linhas) numa fachada enxuta que delega a lógica pura de correspondência textual/temporal e a construção de barras de ferramentas/instaladores Swing a colaboradores coesos no pacote `mz.multicore.erp.gui.components`.

## Colaboradores Extraídos
1. `TableFilterMatcher.java`: Lógica pura e determinística de correspondência de linhas (`rowMatches`, `cellMatches`), datas (`parseCellDate`, `matchesPeriod`, `matchesDateRange`) e normalização diacrítica.
2. `TableFilterBarFactory.java`: Construção de componentes visuais (`searchField`, `combo`, `periodCombo`, `label`, `bar`, `toolbar`, `separator`) e instalação/limpeza de `TableRowSorter` (`install`, `clear`).

## Invariantes Preservadas
- Assinaturas públicas, classes internas (`ContainsColumnFilter`, `ColumnFilter`, `PeriodFilter`, `DateRangeFilter`) e constante `CLEAR_ACTION_KEY` permanecem 100% compatíveis em `TableFilter`.

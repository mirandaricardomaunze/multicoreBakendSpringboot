# Harness de Decomposição: TableFilter (Fase 198)

## Critérios de Verificação
1. `TableFilter.java` reduzido para $\le 140$ linhas.
2. `TableFilterMatcher.java` e `TableFilterBarFactory.java` abaixo de 260 linhas cada.
3. Correspondência textual, equivalência de estados `pt-MZ` (`PENDING` / `Pendente` / `Por faturar`), períodos e barras de filtro validadas por `TableFilterDecompositionHarnessTest`.

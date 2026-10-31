# Harness de Verificação: AccountingReportService (Fase 155)

## Critérios de Aceitação
1. `AccountingReportService.java` $\le 65$ linhas físicas.
2. `LedgerAndTrialBalanceCalculator.java` $\le 140$ linhas físicas.
3. `FinancialStatementBuilder.java` $\le 90$ linhas físicas.
4. Verificação de equilíbrio (`balanced`) no Balancete e no Balanço Patrimonial preservada via `compareTo(...) == 0`.
5. Cobertura validada por `AccountingReportServiceDecompositionHarnessTest`.

# Especificação de Decomposição: AccountingReportService (Fase 155)

## 1. Diagnóstico do Monólito
`AccountingReportService.java` possuía **226 linhas**, misturando:
- Apuramento de Balancete do Período (`getTrialBalance`) e Extracto de Razão por conta com saldo de abertura (`getLedger`).
- Demonstração de Resultados (`getIncomeStatement`) e Balanço Patrimonial (`getBalanceSheet`).

## 2. Arquitectura Alvo
1. `AccountingReportService` (`<= 65` linhas): Fachada `@Service` e `@Transactional(readOnly = true)`, preservando o construtor `(JournalService, AccountRepository)`.
2. `LedgerAndTrialBalanceCalculator` (`<= 140` linhas): Componente `@Component` dedicado ao Balancete, Extracto de Razão e acumulação por conta.
3. `FinancialStatementBuilder` (`<= 90` linhas): Componente `@Component` dedicado à Demonstração de Resultados e Balanço Patrimonial.

# Especificação de Decomposição: FinanceService (Fase 182)

## Objetivo
Decompor `FinanceService.java` (176 linhas) numa fachada de orquestração (`<= 95` linhas) que preserva o lock pessimista de liquidação de faturas (`payInvoice`) e delega as mutações de tesouraria e consultas paginadas para colaboradores coesos.

## Arquitetura Alvo
1. `FinanceService.java` (`<= 95` linhas) — Fachada pública `@Service` preservando `payInvoice` com lock pessimista e construtor legado.
2. `TreasuryOperationService.java` (`<= 95` linhas) — Movimentação manual e automática de contas de tesouraria (`registerTransaction`, `registerAutoExpensePayout`, `registerAutoPayout`, `toDTO`).
3. `TreasuryQueryService.java` (`<= 65` linhas) — Consultas de contas, listagem e paginação server-side de transações (`getAllAccounts`, `getAllTransactions`, `getTransactionsPage`, `accountType`).

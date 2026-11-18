# Harness de Verificação: RecurringContractService (Fase 159)

## Critérios de Aceitação
1. `RecurringContractService.java` $\le 75$ linhas de código.
2. `RecurringContractLifecycleService.java` $\le 130$ linhas de código.
3. `RecurringBatchBillingProcessor.java` $\le 95$ linhas de código; abre uma transacção por período facturado.
4. `RecurringPeriodBiller.java` $\le 95$ linhas de código; bloqueia o contrato (`lockForBilling`), regista o `RecurringBillingLog` e expira o contrato quando `autoRenew == false`.
5. `RecurringContractInvoicer.java` $\le 95$ linhas de código; emite a fatura comercial (`comercialService.createInvoice`).
6. A facturação em lote gera uma fatura com IVA por período vencido e não volta a facturar o mesmo período.

Linhas de código contam-se com `SourceLines.code` (os comentários não contam).

| ID | Critério | Teste |
|----|----------|-------|
| RC-01 | Critérios 1–5 | `RecurringContractServiceDecompositionHarnessTest` |
| RC-02 | Critério 6 | `RecurringBatchBillingHttpIntegrationTest` |
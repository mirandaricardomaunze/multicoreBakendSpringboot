# Harness de Verificação: RecurringContractService (Fase 159)

## Critérios de Aceitação
1. `RecurringContractService.java` $\le 75$ linhas físicas.
2. `RecurringContractLifecycleService.java` $\le 130$ linhas físicas.
3. `RecurringBatchBillingProcessor.java` $\le 95$ linhas físicas.
4. Processamento em lote (`processBatchBilling`), expiração quando `autoRenew == false` e construtor legado preservados.
5. Cobertura validada por `RecurringContractServiceDecompositionHarnessTest`.

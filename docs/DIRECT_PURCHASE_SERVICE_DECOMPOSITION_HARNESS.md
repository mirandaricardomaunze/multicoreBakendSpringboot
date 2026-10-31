# Harness de Verificação: DirectPurchaseService (Fase 157)

## Critérios de Aceitação
1. `DirectPurchaseService.java` $\le 85$ linhas físicas.
2. `DirectPurchaseRegistrationService.java` $\le 145$ linhas físicas.
3. `DirectPurchaseQueryService.java` $\le 75$ linhas físicas.
4. Validação multi-tenant, movimento de stock `PURCHASE` e publicação de `PurchaseRegisteredEvent` preservados.
5. Cobertura validada por `DirectPurchaseServiceDecompositionHarnessTest`.

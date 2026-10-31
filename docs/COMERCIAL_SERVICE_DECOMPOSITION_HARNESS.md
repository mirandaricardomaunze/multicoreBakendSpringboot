# Harness de Verificação — ComercialService (Fase 194)

## Cobertura do Harness (`ComercialServiceDecompositionHarnessTest`)
1. **Delegação:** Verifica que `ComercialService` delega operações de clientes/crédito, catálogo de produtos, faturas e encomendas para `ComercialClientAndCreditService`, `ComercialProductCatalogService`, `ComercialInvoiceLifecycleService` e `ComercialOrderLifecycleService`.
2. **Limites físicos de linhas:**
   - `ComercialService.java` <= 420 linhas (de 2.155 linhas)
   - `ComercialClientAndCreditService.java` <= 230 linhas
   - `ComercialProductCatalogService.java` <= 420 linhas
   - `ComercialInvoiceLifecycleService.java` <= 420 linhas
   - `ComercialOrderLifecycleService.java` <= 550 linhas
3. **Harnesses existentes preservados:** `OrderPaginationHarnessTest`, `InvoicePaymentConcurrencyHarnessTest` e `ComercialServiceTest`.

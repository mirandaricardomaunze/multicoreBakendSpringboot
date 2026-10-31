# Especificação de Decomposição: ReceivablesService (Fase 178)

## Contexto e Problema
`ReceivablesService` (187 linhas) concentrava a verificação de disponibilidade de limite de crédito por cliente (`assertCreditAvailable` / `outstandingTotalFor`) juntamente com o cálculo e agregação do mapa de antiguidade de saldos (`getAging`) por escalões `AgingBucket` e por cliente (`ClientAging`).

## Solução Arquitectural
1. **`ReceivablesService` (Fachada — `<= 65` linhas)**:
   - Orquestra as chamadas transaccionais delegando para `ClientCreditLimitValidator` e `ReceivablesAgingCalculator`.
   - Mantém construtor compatível `ReceivablesService(InvoiceRepository)`.
2. **`ClientCreditLimitValidator` (`@Component` — `<= 65` linhas)**:
   - Gere `outstandingTotalFor` e `assertCreditAvailable`.
3. **`ReceivablesAgingCalculator` (`@Component` — `<= 125` linhas)**:
   - Gere `getAging` e o acumulador `ClientAging`.

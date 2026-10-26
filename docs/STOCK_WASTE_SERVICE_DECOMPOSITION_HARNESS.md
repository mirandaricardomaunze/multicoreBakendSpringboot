# Harness de Testes: Decomposição do StockWasteService (Fase 106)

## 1. Objetivos do Harness
Garantir que a refatoração do `StockWasteService` mantém:
1. Limites físicos de linhas rigorosos:
   - `StockWasteService.java` $\le 135$ linhas.
   - `StockWasteLifecycleService.java` $\le 285$ linhas.
   - `StockWasteAnalyticsService.java` $\le 155$ linhas.
   - `StockWasteQueryService.java` $\le 75$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `StockWasteHarnessTest` (9/9 testes) e `AuditTrailCriticalEventsHarnessTest`.

## 2. Testes Automatizados Executados
- `StockWasteServiceDecompositionHarnessTest`:
  - `wasteServiceUnderLimit`: Valida que `StockWasteService.java` não ultrapassa 125 linhas.
  - `lifecycleServiceUnderLimit`: Valida que `StockWasteLifecycleService.java` não ultrapassa 255 linhas.
  - `analyticsServiceUnderLimit`: Valida que `StockWasteAnalyticsService.java` não ultrapassa 135 linhas.
  - `queryServiceUnderLimit`: Valida que `StockWasteQueryService.java` não ultrapassa 75 linhas.
- `StockWasteHarnessTest`: Cobertura de registo de quebras com auto-aprovação, bloqueio acima de 2.500 MZN, abatimento físico de stock, radar de lotes a expirar e resumo com rácio sobre vendas.
- `AuditTrailCriticalEventsHarnessTest`: Validação de emissão de eventos formais de auditoria para registo, aprovação e rejeição de quebras.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

# Harness de Testes: Decomposição do CRMService (Fase 104)

## 1. Objetivos do Harness
Garantir que a refatoração do `CRMService` mantém:
1. Limites físicos de linhas rigorosos:
   - `CRMService.java` $\le 150$ linhas.
   - `SupportTicketLifecycleService.java` $\le 160$ linhas.
   - `WorkSheetService.java` $\le 265$ linhas.
   - `CRMQueryService.java` $\le 95$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `CRMServiceTest` (10/10 testes).

## 2. Testes Automatizados Executados
- `CRMServiceDecompositionHarnessTest`:
  - `crmServiceUnderLimit`: Valida que `CRMService.java` não ultrapassa 130 linhas.
  - `ticketLifecycleServiceUnderLimit`: Valida que `SupportTicketLifecycleService.java` não ultrapassa 160 linhas.
  - `workSheetServiceUnderLimit`: Valida que `WorkSheetService.java` não ultrapassa 250 linhas.
  - `queryServiceUnderLimit`: Valida que `CRMQueryService.java` não ultrapassa 95 linhas.
- `CRMServiceTest`: Cobertura de criação, anulação de tickets, controlo de catálogo sem stock, cálculos e facturação de folhas de obra.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

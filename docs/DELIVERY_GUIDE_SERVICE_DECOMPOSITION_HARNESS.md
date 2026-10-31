# Harness de Testes: Decomposição do DeliveryGuideService (Fase 111)

## 1. Objetivos do Harness
Garantir que a refatoração do `DeliveryGuideService` mantém:
1. Limites físicos de linhas rigorosos:
   - `DeliveryGuideService.java` $\le 120$ linhas.
   - `DeliveryGuideCreationService.java` $\le 190$ linhas.
   - `DeliveryGuideLifecycleService.java` $\le 160$ linhas.
   - `DeliveryGuideQueryService.java` $\le 145$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `DeliveryGuideServiceTest` (9/9 testes).

## 2. Testes Automatizados Executados
- `DeliveryGuideServiceDecompositionHarnessTest`:
  - `orchestratorUnderLimit`: Valida que `DeliveryGuideService.java` não ultrapassa 120 linhas.
  - `creationServiceUnderLimit`: Valida que `DeliveryGuideCreationService.java` não ultrapassa 190 linhas.
  - `lifecycleServiceUnderLimit`: Valida que `DeliveryGuideLifecycleService.java` não ultrapassa 160 linhas.
  - `queryServiceUnderLimit`: Valida que `DeliveryGuideQueryService.java` não ultrapassa 145 linhas.
- `DeliveryGuideServiceTest`: Cobertura completa de criação de guias (avulsas e a partir de encomendas), verificação de transportador e matrícula, bloqueio de reposição interna, máquina de estados pendente/aprovada/rejeitada/cancelada, saída física de stock (SALE) e libertação de encomendas.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

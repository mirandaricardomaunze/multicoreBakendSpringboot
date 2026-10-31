# Harness de Testes: Decomposição do TerminationService (Fase 110)

## 1. Objetivos do Harness
Garantir que a refatoração do `TerminationService` mantém:
1. Limites físicos de linhas rigorosos:
   - `TerminationService.java` $\le 125$ linhas.
   - `TerminationSettlementCalculator.java` $\le 210$ linhas.
   - `TerminationLifecycleService.java` $\le 145$ linhas.
   - `TerminationQueryService.java` $\le 100$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `TerminationServiceTest` (14/14 testes).

## 2. Testes Automatizados Executados
- `TerminationServiceDecompositionHarnessTest`:
  - `orchestratorUnderLimit`: Valida que `TerminationService.java` não ultrapassa 125 linhas.
  - `calculatorUnderLimit`: Valida que `TerminationSettlementCalculator.java` não ultrapassa 210 linhas.
  - `lifecycleServiceUnderLimit`: Valida que `TerminationLifecycleService.java` não ultrapassa 145 linhas.
  - `queryServiceUnderLimit`: Valida que `TerminationQueryService.java` não ultrapassa 100 linhas.
- `TerminationServiceTest`: Cobertura completa de preview de acerto, rescisão com e sem contrato, apuramento de proporcionais (salário, 13.º, férias, aviso prévio, deduções), pagamento e bloqueio de acerto negativo.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

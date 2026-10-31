# Harness de Testes: Decomposição do OccupationalHealthService (Fase 109)

## 1. Objetivos do Harness
Garantir que a refatoração do `OccupationalHealthService` mantém:
1. Limites físicos de linhas rigorosos:
   - `OccupationalHealthService.java` $\le 120$ linhas.
   - `OccupationalHealthRegistrationService.java` $\le 160$ linhas.
   - `OccupationalHealthFinanceService.java` $\le 145$ linhas.
   - `OccupationalHealthQueryService.java` $\le 160$ linhas.
   - `OccupationalHealthMapper.java` $\le 60$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `OccupationalHealthServiceTest` (10/10 testes).

## 2. Testes Automatizados Executados
- `OccupationalHealthServiceDecompositionHarnessTest`:
  - `orchestratorUnderLimit`: Valida que `OccupationalHealthService.java` não ultrapassa 120 linhas.
  - `registrationServiceUnderLimit`: Valida que `OccupationalHealthRegistrationService.java` não ultrapassa 160 linhas.
  - `financeServiceUnderLimit`: Valida que `OccupationalHealthFinanceService.java` não ultrapassa 145 linhas.
  - `queryServiceUnderLimit`: Valida que `OccupationalHealthQueryService.java` não ultrapassa 160 linhas.
  - `mapperUnderLimit`: Valida que `OccupationalHealthMapper.java` não ultrapassa 60 linhas.
- `OccupationalHealthServiceTest`: Cobertura completa de registo de exames, proteção legal contra dados serológicos, sigilo médico do resumo e anexos, pagamentos e relatórios de custo.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

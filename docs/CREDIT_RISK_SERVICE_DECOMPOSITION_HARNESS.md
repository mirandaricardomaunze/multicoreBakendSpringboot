# Harness de Testes: Decomposição do CreditRiskService (Fase 105)

## 1. Objetivos do Harness
Garantir que a refatoração do `CreditRiskService` mantém:
1. Limites físicos de linhas rigorosos:
   - `CreditRiskService.java` $\le 120$ linhas.
   - `CreditRiskAssessmentService.java` $\le 255$ linhas.
   - `CreditControlService.java` $\le 150$ linhas.
   - `DebtCollectionNoticeService.java` $\le 135$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `CreditRiskHarnessTest` (7/7 testes).

## 2. Testes Automatizados Executados
- `CreditRiskServiceDecompositionHarnessTest`:
  - `creditRiskServiceUnderLimit`: Valida que `CreditRiskService.java` não ultrapassa 120 linhas.
  - `assessmentServiceUnderLimit`: Valida que `CreditRiskAssessmentService.java` não ultrapassa 230 linhas.
  - `controlServiceUnderLimit`: Valida que `CreditControlService.java` não ultrapassa 135 linhas.
  - `noticeServiceUnderLimit`: Valida que `DebtCollectionNoticeService.java` não ultrapassa 120 linhas.
- `CreditRiskHarnessTest`: Cobertura de matriz de aging, classificação de risco, bloqueio de crédito, excepções de crédito e geração de notificações de cobrança.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

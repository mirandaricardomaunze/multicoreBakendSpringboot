# Harness de Testes: Decomposição do BankReconciliationService (Fase 107)

## 1. Objetivos do Harness
Garantir que a refatoração do `BankReconciliationService` mantém:
1. Limites físicos de linhas rigorosos:
   - `BankReconciliationService.java` $\le 110$ linhas.
   - `BankReconciliationMatcher.java` $\le 240$ linhas.
   - `BankReconciliationQueryService.java` $\le 175$ linhas.
   - `BankStatementImportService.java` $\le 120$ linhas.
2. Integridade funcional e conformidade multi-módulo (`MultiModuleArchitectureHarnessTest`).
3. Total cobertura do comportamento anterior validado em `BankReconciliationHarnessTest` (7/7 testes).

## 2. Testes Automatizados Executados
- `BankReconciliationServiceDecompositionHarnessTest`:
  - `reconciliationServiceUnderLimit`: Valida que `BankReconciliationService.java` não ultrapassa 110 linhas.
  - `matcherUnderLimit`: Valida que `BankReconciliationMatcher.java` não ultrapassa 200 linhas.
  - `queryServiceUnderLimit`: Valida que `BankReconciliationQueryService.java` não ultrapassa 155 linhas.
  - `importServiceUnderLimit`: Valida que `BankStatementImportService.java` não ultrapassa 120 linhas.
- `BankReconciliationHarnessTest`: Cobertura de importação de extrato, auto-matching por referência e data/valor, matching manual, unmatch, lançamento e conciliação de encargos bancários, encerramento de extrato e resumo de reconciliação.
- `MultiModuleArchitectureHarnessTest`: Garante isolamento estrito entre `contracts`, `backend` e `desktop`.

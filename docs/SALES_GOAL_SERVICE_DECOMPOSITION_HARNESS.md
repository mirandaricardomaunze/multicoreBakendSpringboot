# Harness de Testes - Decomposição do SalesGoalService

## 1. Objectivos do Teste
Validar os limites físicos de linhas de código, a aderência à separação de responsabilidades (SOLID SRP) e a integridade de delegação do serviço de metas e prémios comerciais.

## 2. Critérios de Aceitação
1. `SalesGoalService.java` deve ter $\le 160$ linhas.
2. `SalesGoalLifecycleService.java` deve ter $\le 215$ linhas.
3. `SalesGoalProgressCoordinator.java` deve ter $\le 130$ linhas.
4. `SalesGoalBonusService.java` deve ter $\le 240$ linhas.
5. `SalesGoalQueryService.java` deve ter $\le 95$ linhas.
6. Os 5 componentes devem estar no pacote `mz.multicore.erp.modules.performance.service`.
7. Nenhuma dependência externa deve violar as regras de desacoplamento Maven (`contracts`, `backend`, `desktop`).
8. Todos os testes existentes (`CommercialPerformanceHarnessTest`) devem passar sem regressões.

# Harness de Testes - Decomposição do TimeSheetService

## 1. Objectivos do Teste
Validar os limites físicos de linhas de código, a aderência à separação de responsabilidades (SOLID SRP) e a integridade de delegação do serviço de ponto e assiduidade.

## 2. Critérios de Aceitação
1. `TimeSheetService.java` deve ter $\le 180$ linhas.
2. `TimeEntryRegistrationService.java` deve ter $\le 160$ linhas.
3. `TimeSheetCalculator.java` deve ter $\le 195$ linhas.
4. `TimeSheetPeriodCoordinator.java` deve ter $\le 195$ linhas.
5. Os 4 componentes devem estar no pacote `mz.multicore.erp.modules.hr.service`.
6. Nenhuma dependência externa deve violar as regras de desacoplamento Maven (`contracts`, `backend`, `desktop`).
7. Todos os testes unitários legados (`TimeSheetServiceTest`, `OvertimeValuationServiceTest`, `HRServiceTest`) devem passar sem modificações.

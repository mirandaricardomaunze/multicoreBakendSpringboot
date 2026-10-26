# Harness de Testes - Decomposição do AutomaticPostingService

## 1. Objectivos do Teste
Validar os limites físicos de linhas de código, a aderência à separação de responsabilidades (SOLID SRP) e a integridade de escrituração contabilística por partidas dobradas.

## 2. Critérios de Aceitação
1. `AutomaticPostingService.java` deve ter $\le 160$ linhas.
2. `CommercialPostingHandler.java` deve ter $\le 195$ linhas.
3. `ProcurementPostingHandler.java` deve ter $\le 140$ linhas.
4. `OperationsPostingHandler.java` deve ter $\le 195$ linhas.
5. `PostingHelper.java` deve ter $\le 95$ linhas.
6. Os 5 componentes devem estar no pacote `mz.multicore.erp.modules.accounting.service`.
7. `AutomaticPostingService.java` deve conter `@EventListener` e referenciar `PayslipPaidEvent` e `PayrollLiabilityDeliveredEvent`, sem importar entidades JPA de RH.
8. Todos os testes unitários (`AutomaticPostingServiceTest`, `HrDoesNotKnowAccountingTest`) devem passar sem modificações.

# Harness de Testes - Decomposição do QuotationService

## 1. Objectivos do Teste
Validar os limites físicos de linhas de código, a aderência à separação de responsabilidades (SOLID SRP) e a integridade de delegação do serviço de propostas e cotações.

## 2. Critérios de Aceitação
1. `QuotationService.java` deve ter $\le 160$ linhas.
2. `QuotationLifecycleService.java` deve ter $\le 295$ linhas.
3. `QuotationConversionService.java` deve ter $\le 150$ linhas.
4. `QuotationQueryService.java` deve ter $\le 135$ linhas.
5. Os 4 componentes devem estar no pacote `mz.multicore.erp.modules.comercial.service`.
6. Nenhuma dependência externa deve violar as regras de desacoplamento Maven (`contracts`, `backend`, `desktop`).
7. Todos os testes existentes (`QuotationServiceTest`, `QuotationConversionHarnessTest`) devem passar sem regressões.

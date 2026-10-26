# Harness de Testes - Decomposição do SubscriptionService

## 1. Objectivos do Teste
Validar os limites físicos de linhas de código, a aderência à separação de responsabilidades (SOLID SRP) e a integridade de delegação do serviço de assinaturas da plataforma.

## 2. Critérios de Aceitação
1. `SubscriptionService.java` deve ter $\le 160$ linhas.
2. `SubscriptionLifecycleService.java` deve ter $\le 195$ linhas.
3. `SubscriptionPaymentService.java` deve ter $\le 150$ linhas.
4. `SubscriptionRenewalService.java` deve ter $\le 255$ linhas.
5. `SubscriptionQueryService.java` deve ter $\le 95$ linhas.
6. Os 5 componentes devem estar no pacote `mz.multicore.erp.modules.subscription.service`.
7. Nenhuma dependência externa deve violar as regras de desacoplamento Maven (`contracts`, `backend`, `desktop`).
8. Todos os testes existentes (`SubscriptionServiceTest`, `PlatformSubscriptionPaymentsHarnessTest`, `SecurityAttackSurfaceTest`) devem passar sem modificações.

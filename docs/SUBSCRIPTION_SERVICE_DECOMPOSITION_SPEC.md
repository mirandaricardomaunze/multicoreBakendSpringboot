# Especificação de Decomposição - SubscriptionService

## 1. Contexto e Motivação
O `SubscriptionService` (462 linhas) gere assinaturas e licenciamento multi-tenant da plataforma Multicore ERP (superadmin e self-service). Ele concentra quatro responsabilidades distintas:
1. **Ciclo de Vida da Assinatura:** Definição administrativa de planos (`saveSubscription`), suspensão/reactivação (`changeStatus`), política de bloqueio de login (`allowsLogin`) e consulta pelo próprio assinante (`getMySubscription`).
2. **Gestão de Pagamentos Administrativos:** Registo de pagamentos com extensão de vigência (`recordPayment`) e histórico documental (`listPayments`).
3. **Renovação e Pagamentos Móveis Self-Service:** Catálogo de planos (`listAvailablePlans`), cálculo de preços com descontos progressivos (`calculateRenewalPrice`), e processamento de pagamentos móveis M-Pesa/e-Mola (`initiateSelfServiceRenewal`, `confirmMobileRenewal`).
4. **Visão Global e Consultas:** Listagem consolidada de empresas e estado de licenciamento (`listOverview`, `findByCompany`).

## 2. Nova Arquitectura Modular
O `SubscriptionService` passa a actuar como orquestrador enxuto ($\le 160$ linhas) delegando para 4 serviços especializados no pacote `mz.multicore.erp.modules.subscription.service`:

1. `SubscriptionLifecycleService` ($\le 195$ linhas):
   - Gestão de vigência, planos e política de controlo de acesso de login por empresa.
2. `SubscriptionPaymentService` ($\le 150$ linhas):
   - Registo manual de pagamentos e histórico financeiro de licenças.
3. `SubscriptionRenewalService` ($\le 255$ linhas):
   - Motor de renovação self-service, cálculo de descontos e integração com gateways móveis (`MobilePaymentService`).
4. `SubscriptionQueryService` ($\le 95$ linhas):
   - Consultas de visão geral (superadmin) e mapeamento para DTOs.

## 3. Compatibilidade e Regras
- Preservar os construtores de 4 e 6 parâmetros em `SubscriptionService` para compatibilidade com `SubscriptionServiceTest` e `PlatformSubscriptionPaymentsHarnessTest`.
- Proteger operações de plataforma com `PermissionGuard.requireSuperAdmin`.
- Manter mensagens de erro e logs em português de Moçambique (pt-MZ).

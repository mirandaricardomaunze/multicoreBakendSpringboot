# Especificação de Decomposição: InvoiceInstallmentService (Fase 188)

## Objetivo
Decompor `InvoiceInstallmentService.java` (168 linhas) numa fachada enxuta (`<= 65` linhas) que delega a geração de planos de prestações e a amortização/consulta de prestações para colaboradores coesos.

## Arquitetura Alvo
1. `InvoiceInstallmentService.java` (`<= 65` linhas) — Fachada `@Service` preservando `toDTO` e o construtor legado.
2. `InstallmentPlanGenerator.java` (`<= 125` linhas) — Validação, divisão proporcional/personalizada de valores e criação de prestações com amortização de pagamentos prévios (`createInstallmentPlan`, `toDTO`).
3. `InstallmentPaymentAmortizer.java` (`<= 60` linhas) — Listagem de prestações e aplicação de pagamentos sequenciais (`getInstallments`, `applyPaymentToInvoiceInstallments`).

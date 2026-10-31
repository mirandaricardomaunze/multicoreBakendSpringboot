# Especificação de Decomposição: RecurringContractService (Fase 159)

## 1. Diagnóstico do Monólito
`RecurringContractService.java` possuía **221 linhas**, misturando:
- Gestão de contratos de avença e facturação periódica (`listContracts`, `getContract`, `createContract`, `updateStatus`, cálculo de `initialNextBilling` e mapeamento DTO).
- Motor de processamento em lote (`processBatchBilling`), geração de `RecurringBillingLog`, avanço de `nextBillingDate` e expiração automática.

## 2. Arquitectura Alvo
1. `RecurringContractService` (`<= 75` linhas): Fachada `@Service` e `@Transactional`, preservando o construtor de 4 argumentos.
2. `RecurringContractLifecycleService` (`<= 130` linhas): Componente `@Component` dedicado à listagem, consulta, criação, actualização de estado e mapeamento DTO.
3. `RecurringBatchBillingProcessor` (`<= 95` linhas): Componente `@Component` dedicado ao processamento em lote de facturação periódica e cálculo da próxima data.

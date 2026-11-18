# Especificação de Decomposição: RecurringContractService (Fase 159)

## 1. Diagnóstico do Monólito
`RecurringContractService.java` possuía **221 linhas**, misturando:
- Gestão de contratos de avença e facturação periódica (`listContracts`, `getContract`, `createContract`, `updateStatus`, cálculo de `initialNextBilling` e mapeamento DTO).
- Motor de processamento em lote (`processBatchBilling`), geração de `RecurringBillingLog`, avanço de `nextBillingDate` e expiração automática.

## 2. Arquitectura Alvo
1. `RecurringContractService` (`<= 75` linhas): Fachada `@Service`. O lote não é `@Transactional`: cada período tem a sua transacção.
2. `RecurringContractLifecycleService` (`<= 130` linhas): Componente `@Component` dedicado à listagem, consulta, criação, actualização de estado e mapeamento DTO.
3. `RecurringBatchBillingProcessor` (`<= 95` linhas): percorre os contratos vencidos e factura cada período em atraso numa transacção própria (máximo de 24 por contrato e por execução). Uma falha fica registada e não desfaz os períodos já facturados nem pára os outros contratos.
4. `RecurringPeriodBiller` (`<= 95` linhas): factura um período com o contrato bloqueado, regista o `RecurringBillingLog` com a fatura, avança a próxima data e expira o contrato que terminou sem renovação automática.
5. `RecurringContractInvoicer` (`<= 95` linhas): emite a fatura comercial do período através de um produto de serviço sem stock por contrato (`AVENCA-<empresa>-<contrato>`). O valor fixo do contrato é **sem IVA**: a fatura acrescenta a taxa normal (16%).

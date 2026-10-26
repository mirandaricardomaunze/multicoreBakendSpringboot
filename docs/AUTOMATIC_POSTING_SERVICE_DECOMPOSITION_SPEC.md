# Especificação de Decomposição - AutomaticPostingService

## 1. Contexto e Motivação
O `AutomaticPostingService` (503 linhas) traduz eventos de negócio em lançamentos contabilísticos por partidas dobradas em tempo real (PGC-NIRF). Ele concentra os lançamentos de 4 áreas de negócio:
1. **Comercial e Vendas:** Vendas faturadas, recebimentos de clientes, notas de crédito e notas de débito.
2. **Compras e Fornecedores:** Compras de mercadorias e pagamentos a fornecedores.
3. **Operações, RH e Activos:** Salários pagos, entrega de retenções ao Estado, conclusão de ordens de produção, quotas de depreciação de activos e consumo de combustível da frota.

## 2. Nova Arquitectura Modular
O `AutomaticPostingService` actua como orquestrador de eventos ($\le 160$ linhas) contendo os listeners `@EventListener` síncronos e delegando a escrituração para 3 handlers especializados e 1 utilitário de suporte no pacote `mz.multicore.erp.modules.accounting.service`:

1. `CommercialPostingHandler` ($\le 195$ linhas):
   - Escrituração de Vendas (`postSale`), Recebimentos (`postPayment`), Notas de Crédito (`postCreditNote`) e Notas de Débito (`postDebitNote`).
2. `ProcurementPostingHandler` ($\le 140$ linhas):
   - Escrituração de Compras de mercadorias (`postPurchase`) e Pagamentos a Fornecedores (`postSupplierPayment`).
3. `OperationsPostingHandler` ($\le 195$ linhas):
   - Escrituração de Salários (`postPayslip`), Entrega de Retenções fiscais (`postPayrollLiability`), Ordens de Produção (`postProduction`), Depreciação de Activos (`postAssetDepreciation`) e Combustível de Frotas (`postFleetFuel`).
4. `PostingHelper` ($\le 95$ linhas):
   - Validações de plano de contas activo (`canPost`), prevenção de lançamentos duplicados (`alreadyPosted`), resolução de contas de tesouraria (`cashAccount`) e contas do PGC-NIRF (`account`).

## 3. Compatibilidade e Regras
- Manter o construtor sobrecarregado com 3 parâmetros `(ChartOfAccountsService, JournalService, AuditLogService)` em `AutomaticPostingService` para retrocompatibilidade com testes legados (`AutomaticPostingServiceTest`).
- Preservar os nomes de eventos e anotações `@EventListener` em `AutomaticPostingService` para conformidade estrita com `HrDoesNotKnowAccountingTest` e com o Spring Event Publisher.
- Nenhuma dependência directa do modelo ou serviços de RH (`modules.hr.*`).

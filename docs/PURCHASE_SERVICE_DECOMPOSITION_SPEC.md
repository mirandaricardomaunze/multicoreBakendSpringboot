# Especificação Técnica: Decomposição Modular do PurchaseService (Fase 112)

## 1. Contexto e Diagnóstico
O `PurchaseService.java` acumulava 418 linhas de código no backend (`mz.multicore.erp.modules.purchases.service`), agrupando três responsabilidades críticas de aprovisionamento, inventário e tesouraria:
- **Gestão de Fornecedores:** Validação NUIT (`TaxIdValidator`), criação com empresa vinculada, edição, ativação/desativação restrita a gestores/administradores, pesquisa fonética e projeções DTO.
- **Registo e Escrituração de Compras Diretas:** Validações de fornecedor e armazém da mesma empresa, cálculo de IVA efetivo do produto via `LineCalculator`, entrada física no inventário via `InventoryService.registerMovement` (tipo "PURCHASE" com lote e validade), numeração sequencial oficial (`DocumentSeries.PURCHASE`), liquidação imediata em conta de tesouraria quando aplicável e emissão do evento `PurchaseRegisteredEvent`.
- **Contas a Pagar e Pagamentos a Fornecedores:** Apuração de compras a crédito com saldo em dívida (`PayableDTO`), validação estrita de valores positivos e teto de dívida, liquidação por crédito em tesouraria via `FinanceService.registerTransaction`, disparo de `SupplierPaymentRegisteredEvent` e registo formal de auditoria.

## 2. Decisão Arquitetural
A decomposição segrega o serviço em 3 colaboradores especializados com responsabilidade única (SRP):

1. **`SupplierManagementService.java`** (136 linhas $\le 150$):
   - Cadastro e ciclo de vida de fornecedores (`createSupplier`, `updateSupplier`, `setSupplierActive`).
   - Validação de NUIT e filtros de pesquisa por empresa ativa (`searchSuppliers`, `findSuppliersByCompany`).
   - Mapeamento determinístico `toDTO(Supplier)`.

2. **`DirectPurchaseService.java`** (224 linhas $\le 240$):
   - Execução da compra direta (`createPurchase` / `createPurchaseDTO`): validação de fornecedor e armazém, apreçamento com IVA, entrada no stock com lotes e datas de validade, atribuição de número de série e pagamento à vista.
   - Disparo do evento `PurchaseRegisteredEvent` para escrituração contabilística automática.
   - Listagens de faturas de compra por empresa e projeção `toDTO(Purchase)`.

3. **`SupplierPaymentService.java`** (114 linhas $\le 130$):
   - Carteira de contas a pagar a fornecedores (`findPayablesByCompany`) com determinação de unidade de negócio.
   - Liquidação transacional de pagamentos (`registerSupplierPayment`) com saída de tesouraria, evento `SupplierPaymentRegisteredEvent` e auditoria.

4. **`PurchaseService.java`** (160 linhas $\le 175$):
   - Fachada unificada que delega nos três serviços especializados.
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 10 dependências garantindo 100% de compatibilidade com os testes unitários (`PurchaseServiceTest`, `MulticoreServicesTest`) e controladores REST (`PurchaseController`).

## 3. Conformidade
- Redução de 418 para 160 linhas no orquestrador (redução de 258 linhas / 61.7%).
- 100% retrocompatível com `PurchaseServiceTest`, `PurchaseController` e `MulticoreServicesTest`.
- Zero uso de `BigDecimal.ZERO.equals`.
- Mensagens ao utilizador em português de Moçambique.

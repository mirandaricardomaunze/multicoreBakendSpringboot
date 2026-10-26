# Especificação Técnica — Decomposição Modular de PurchaseOrderService (Fase 98)

## 1. Contexto e Diagnóstico

`PurchaseOrderService.java` acumulava mais de 522 linhas e 10 dependências diretas, combinando sob a mesma classe:
1. Validações e ciclo comercial (criação de encomenda, renegociação de preços/linhas antes de receber e anulação com motivo);
2. Processamento transacional de recepção física (total e parcial), conferência com detecção de mercadorias danificadas ou em falta (fecho curto), idempotência SHA-256 e integração de stock com `inventoryService.registerMovement`.

## 2. Princípios Arquiteturais e Diretrizes Aplicadas

- **SOLID - Princípio da Responsabilidade Única (SRP):** Decomposição em ciclo de vida (`PurchaseOrderLifecycleService`) e execução de recepção (`PurchaseOrderReceiptExecutor`).
- **Injeção de Dependências por Construtor:** Sub-serviços anotados com `@Service` e injetados via construtor com campos `final`.
- **Preservação de Construtores Retrocompatíveis:** Construtor com 10 parâmetros mantido intacto para testes unitários com mocks.
- **Limites Físicos de Linhas:**
  - `PurchaseOrderService.java`: $\le 195$ linhas (orquestrador enxuto de ~183 linhas).
  - `PurchaseOrderLifecycleService.java`: $\le 220$ linhas (~212 linhas).
  - `PurchaseOrderReceiptExecutor.java`: $\le 250$ linhas (~239 linhas).

## 3. Estrutura Modular Decomposta

```
backend/src/main/java/mz/multicore/erp/modules/purchases/service/
├── PurchaseOrderService.java           (Orquestrador enxuto, ~183 linhas)
├── PurchaseOrderLifecycleService.java  (Emissão, alteração e cancelamento, ~168 linhas)
└── PurchaseOrderReceiptExecutor.java   (Recepções, divergências e stock, ~186 linhas)
```

## 4. Colaboradores Especializados

### 4.1 PurchaseOrderLifecycleService
- `createOrder(CreatePurchaseOrderRequest)`: Valida fornecedor, armazém, calcula linhas e impostos (`LineCalculator.compute`), numeração sequencial e log de auditoria.
- `updateOrder(PurchaseOrder, UpdatePurchaseOrderRequest)`: Validação de versão concorrente e edição de linhas antes de qualquer recepção.
- `cancelOrder(PurchaseOrder, String)`: Cancelamento seguro com permissão `requireManagerOrAdmin` e registo em auditoria.

### 4.2 PurchaseOrderReceiptExecutor
- `receiveOrder(PurchaseOrder)`: Recepção total do saldo pendente com entrada em inventário ("PURCHASE").
- `receivePartial(Long, ReceivePurchaseOrderRequest)`: Recepção fracionada com bloqueio pessimista `lockByIdAndCompanyId` e idempotência SHA-256.
- `recordDiscrepancies`: Separação de danos (`DAMAGED`) de quebras (`MISSING`) com suporte a fecho curto.

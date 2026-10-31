# Especificação Técnica: Decomposição Modular do StockTransferTransitService (Fase 120)

## 1. Contexto e Diagnóstico
O `StockTransferTransitService.java` (329 linhas, `mz.multicore.erp.modules.inventory.service`) acumulava:
- Despacho na origem por FEFO com registo de parcelas por lote (`dispatch`).
- Recepção conferida no destino com idempotência SHA-256, bloqueio pessimista da guia, entrada no destino e devolução da diferença à origem com o mesmo lote/validade (`receive`).
- Validações puras da recepção (linhas que não pertencem à guia, duplicadas, quantidade negativa/excedente, motivo de divergência obrigatório).
- Escrita física no razão de stock (movimentos `TRANSFER` e ajuste de saldo com `findForReservation`).
- Consulta de guias em trânsito (`findInTransit`).

## 2. Decisão Arquitetural
Comportamento 100% preservado (código movido 1:1); invariante `saída da origem = entrada no destino + em trânsito` intacta.

1. **`StockTransitLedger.java`** ($\le 80$): movimentos `TRANSFER` por perna e `adjustStock` com bloqueio pessimista (`Stock stock = stockRepository.findForReservation`).
2. **`StockTransferDispatchService.java`** ($\le 85$): despacho FEFO, parcelas `StockTransferLineBatch`, estado `IN_TRANSIT` e auditoria `STOCK_TRANSFER_DISPATCH`.
3. **`StockTransferReceiptValidator.java`** ($\le 105$): funções puras `receiptFingerprint`, `indexReceiptLines`, `validateReceivedQuantity`.
4. **`StockTransferReceiptService.java`** ($\le 155$): `PermissionGuard.requireManagerOrAdmin`, `lockByIdAndCompanyId`, idempotência, entrada/devolução por parcela, estados `RECEIVED` / `RECEIVED_WITH_DISCREPANCY` e auditoria.
5. **`StockTransferTransitService.java`** ($\le 110$): fachada com a API pública inalterada (`dispatch`, 3 sobrecargas de `receive`, `findInTransit`).
   - Construtor canónico `@Autowired` de 3 colaboradores.
   - Construtor retrocompatível de 5 dependências preservado (`StockTransferInTransitHarnessTest`, `StockTransferReceiptDiscrepancyHarnessTest`).

## 3. Conformidade
- `MultiUserRobustnessHarnessTest` passa a verificar o lock pessimista no novo dono (`StockTransitLedger.java`).
- `StockTransferService`, `StockTransferMovementExecutor` e `StockTransferController` sem alterações.
- Zero `BigDecimal.ZERO.equals`; mensagens em português de Moçambique.

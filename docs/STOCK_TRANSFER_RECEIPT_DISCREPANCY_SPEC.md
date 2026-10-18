# Recepção de Transferências em Trânsito com Divergências — Spec

## 1. Problema
No circuito de stock em trânsito (transferência em 2 passos, `docs/STOCK_IN_TRANSIT_SPEC.md`), a mercadoria
despachada viajava com lotes e validades associados à guia. No entanto, a confirmação de recepção no
destino era "tudo ou nada" (`POST /api/inventory/transfers/{id}/receive`). Quando ocorriam faltas físicas,
avarias de transporte ou extravios na descarga, não era possível registar a quantidade real recebida: ou o
destino aceitava stock fantasma que nunca chegou, ou a guia ficava retida em trânsito sem conclusão.

## 2. Decisão de Negócio e Invariante
- **Regra canónica:** O stock que realmente chegou é creditado no armazém de destino. A diferença entre o
  despachado e o recebido **retorna imediatamente ao armazém de origem**, mantendo rigorosamente os mesmos lotes
  e datas de validade com que foi despachado.
- **Invariante permanente:**
  $$\text{Despachado da Origem} = \text{Recebido no Destino} + \text{Devolvido à Origem}$$
  Nenhum stock se perde no éter nem é duplicado. O rácio de conservação de massas é rigorosamente 100%.

## 3. Regras de Funcionamento

1. **Conferência por Linha:**
   - Na recepção, o operador do destino confere as quantidades físicas recebidas por cada linha.
   - A quantidade recebida deve ser $\ge 0$ e $\le$ quantidade enviada. Excedentes físicos não podem ser
     aceites nesta guia (devem ser tratados via entrada de stock avulsa).
   - Se a quantidade recebida for menor que a enviada, é **obrigatório** indicar o motivo da divergência
     (ex.: falta na descarga, mercadoria danificada em viagem, embalagem violada).

2. **Movimentação Automática de Stock e Lotes:**
   - As parcelas em viagem (`StockTransferLineBatch`) são consumidas na ordem de despacho (FEFO):
     - A parcela recebida gera movimento de `TRANSFER` positivo ("entrada") no armazém de destino,
       acrescentando ao lote de destino (`ProductBatchService.addToBatch`).
     - A parcela divergente gera movimento de `TRANSFER` positivo ("devolução à origem por divergência") no
       armazém de origem, reintegrando o lote original.

3. **Ciclo de Vida e Estado:**
   - Se todas as linhas forem recebidas na totalidade $\rightarrow$ estado `RECEIVED` ("Recebido", verde).
   - Se houver qualquer divergência em pelo menos uma linha $\rightarrow$ estado `RECEIVED_WITH_DISCREPANCY`
     ("Recebido c/ divergência", badge âmbar `UIHelper.PENDING_YELLOW`).
   - Ambos os estados são terminais: não admitem cancelamento, rejeição nem dupla recepção.
   - Guias em `RECEIVED_WITH_DISCREPANCY` mantêm autorização para emissão da Guia Oficial de Transporte (3 vias).

4. **Auditoria:**
   - Em caso de divergência, é registado o evento de auditoria `STOCK_TRANSFER_RECEIVE_DISCREPANCY` contendo o
     armazém de destino, a lista discriminada de artigos e quantidades devolvidos à origem, e o motivo declarado.

5. **Interface Desktop:**
   - Diálogo `StockTransferReceivingDialog` (`ModernFormDialog`) com:
     - Cartão superior de cabeçalho: Guia, Origem, Destino e Transportador/Matrícula.
     - Tabela de conferência com recálculo automático em tempo real da devolução ao alterar a coluna "A Receber".
     - `table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE)` prevenindo compressão vertical.
     - Campo inferior de observações gerais da recepção.
   - Filtro de tabela de transferências no `StockPanel` atualizado com "Recebido c/ divergência".
   - Contagem no KPI de transferências concluídas inclui `RECEIVED_WITH_DISCREPANCY`.

## 4. Contratos e Persistência
- **Migração BD:** `V93__stock_transfer_receipt_discrepancy.sql`:
  - `stock_transfer_lines`: colunas `received_quantity`, `returned_quantity`, `discrepancy_reason`.
  - `stock_transfer_line_batches`: coluna `received_quantity`.
  - `stock_transfers.status`: tamanho alinhado com VARCHAR(30).
- **Contratos (`contracts`):**
  - `ReceiveStockTransferRequest`: campos `notes` e `List<ReceiveLine> lines` (construtor de retrocompatibilidade
    `ReceiveStockTransferRequest(String notes)` mantido).
  - `StockTransferLineDTO`: campos `receivedQuantity`, `returnedQuantity`, `discrepancyReason`, método
    `hasDiscrepancy()`, e construtor retrocompatível de 12 argumentos preservado.

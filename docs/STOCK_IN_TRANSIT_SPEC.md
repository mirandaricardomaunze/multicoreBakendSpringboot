# Stock em Trânsito (transferência em 2 passos) - Spec

## 1. Problema
Uma guia de transferência movia o stock origem -> destino num único passo (na aprovação). Enquanto a
mercadoria viaja, o stock desaparecia da origem e já "existia" no destino, sem confirmação de que
chegou. Não havia como mostrar "ainda em trânsito" nem responsabilizar o destino pela recepção.

## 2. Regras
- **R1 Opt-in por guia.** Campo `requiresReceipt` (default `false`). Sem ele, o fluxo de 1 passo e a
  reposição interna ficam inalterados.
- **R2 Despacho na aprovação.** Aprovar uma guia com `requiresReceipt` retira o stock da origem (FEFO)
  e passa a guia a `IN_TRANSIT`. O destino **não** é tocado. `approvedBy/At` e `dispatchedAt/By` gravados.
- **R3 Invariante.** `saída da origem = entrada no destino + em trânsito`. Os lotes em viagem ficam em
  `stock_transfer_line_batches`.
- **R4 Recepção.** `POST /api/inventory/transfers/{id}/receive` só aceita `IN_TRANSIT`, da empresa
  activa, por MANAGER/ADMIN. Credita o destino pelos lotes em viagem (mesma validade) e passa a
  `RECEIVED`, gravando `receivedAt/By` e notas opcionais. Dupla recepção é recusada.
- **R5 Sem retrocesso na v1.** `IN_TRANSIT` não pode ser cancelada nem rejeitada (a devolução à
  origem fica para fase futura).
- **R6 Impressão.** A guia oficial pode ser emitida em `APPROVED`, `IN_TRANSIT` e `RECEIVED`.
- **R7 Auditoria.** `STOCK_TRANSFER_DISPATCH` e `STOCK_TRANSFER_RECEIVE`.
- **R8 Desacoplamento.** `StockTransferTransitService` é colaborador separado, invocado por
  `StockTransferService` só quando `requiresReceipt`; `StockTransferDtoMapper` (sem estado) é partilhado
  para nenhum serviço depender do outro. Construtores antigos mantêm-se.
- **R9 Desktop.** Estado "Em trânsito" (azul) / "Recebido" (verde); KPI "Em Trânsito"; acção
  "Confirmar Recepção" (menu de documentos, máx. 5 opções) com alertas orientadores; checkbox no
  editor da guia (default marcado) "Exigir confirmação de recepção no destino".

## 3. Contratos (retrocompatíveis)
- `CreateStockTransferRequest` / `UpdateStockTransferRequest` += `Boolean requiresReceipt` (ctor de 9 mantido).
- `StockTransferDTO` += `requiresReceipt, dispatchedAt, receivedBy, receivedAt, receiptNotes` (ctor de 23 mantido).
- `ReceiveStockTransferRequest(String notes)` opcional.
- `GET /api/inventory/transfers/in-transit?companyId=&destinationWarehouseId=`.

## 4. Fora de âmbito
Recepção parcial/divergências, devolução à origem, separação de funções despachante != receptor.

## 5. Harness
- `StockTransferInTransitHarnessTest` (backend): despacho só tira da origem; recepção só põe no destino;
  invariante; dupla recepção recusada; não-manager recusado; cancelar/rejeitar em trânsito recusados;
  `requiresReceipt=false` mantém 1 passo.
- `StockInTransitUiHarnessTest` (desktop): rótulo/cor do estado, checkbox no pedido, menu <= 5 opções.

# Histórico POS: valor entregue, troco e cliente não cadastrado

Estado: implementado em 2026-11-05.
Harness: `PosSalePaymentQueryServiceTest` (backend) e `PosSalesHistoryPaymentHarnessTest` (desktop).

## Diagnóstico

Relato: "o POS não está a gravar o valor pago, o troco e o nome do cliente não cadastrado".

A base de dados mostrou que a gravação está correcta. Venda FT-2026/1 de 2026-11-05 21:37:
`invoices.customer_name = 'Miranda'` e `payment_entries`, com CASH 1460,00, entregue 2000,00 e troco 540,00.
O talão também os imprime (`ReceiptDocumentRenderer`).

A falha estava na leitura:

1. O histórico de vendas POS mostrava `InvoiceDTO.clientName`, que numa venda a cliente não cadastrado é
   o registo genérico "Consumidor Final". O nome escrito no caixa está em `customerName`.
2. O valor entregue e o troco vivem em `PaymentEntry` (módulo `pos`) e nenhum endpoint os devolvia.
   Por isso nenhum ecrã os mostrava.

Os campos não foram acrescentados ao `InvoiceDTO`: o módulo `comercial` não depende de `pos` (a dependência
é a inversa), e o `ObjectMapper` do desktop rejeita campos desconhecidos, o que partiria versões antigas.

## Regras

| Id | Regra |
|----|-------|
| PHP-01 | Onde se mostra o comprador de uma fatura, usa-se `InvoiceDTO.buyerName()`: `customerName` quando preenchido, senão `clientName`. A lista de faturas do Comercial mantém o formato "Consumidor Final — Nome". |
| PHP-02 | `GET /api/pos/sales/payments?invoiceIds=…` devolve `PosSalePaymentSummaryDTO(invoiceId, tenderedAmount, changeGiven)`, somado sobre os pagamentos de cada venda. Pagamentos sem valor entregue contam o próprio valor; o troco ausente conta zero. |
| PHP-03 | A consulta exige perfil de vendedor, filtra pela empresa activa e aceita no máximo 200 vendas por pedido. |
| PHP-04 | O histórico de vendas POS tem as colunas Entregue e Troco a seguir a Total. Se a consulta falhar, o histórico carrega à mesma e essas colunas mostram "—". |

Não muda a gravação da venda, o cálculo do troco nem o talão.

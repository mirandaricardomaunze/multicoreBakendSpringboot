# Dívida da recepção e plano de contas — Especificação e harness

**Última actualização:** 2026-11-02

## Objectivo

A revisão com dados de exemplo mostrou dois buracos. Receber uma encomenda aumentava o stock, mas não
criava dívida ao fornecedor, por isso não havia nada para pagar nem lançamento contabilístico. E uma
empresa nova não recebia plano de contas, por isso as suas vendas ficavam fora da contabilidade, sem
erro.

## Decisões

- Cada recepção com mercadoria aceite cria uma compra ligada à encomenda, com o preço e o IVA da
  encomenda. É essa compra que aparece nas contas a pagar, no extrato do fornecedor e no IVA dedutível.
- Só conta a mercadoria aceite. Danificados e em falta não geram dívida.
- Duas recepções parciais criam duas compras, cada uma pelo que chegou nessa vez.
- A compra da recepção não dá entrada de stock, porque o stock já entrou na recepção.
- A recepção e a dívida gravam-se juntas: se uma falhar, nenhuma fica.
- Repetir uma recepção com a mesma chave de idempotência não cria nova dívida.
- A compra directa fica para mercadoria sem encomenda e continua a dar entrada de stock.
- Criar uma empresa cria o plano de contas na mesma transacção.
- No arranque, as empresas sem plano recebem-no. Quem já tem plano não muda.

## Cenários

| ID | Cenário | Resultado |
|----|---------|-----------|
| CD-01 | Receber 10 a 25 MT com IVA de 16% | Dívida de 290 MT, ligada à encomenda |
| CD-02 | Recepção da encomenda | Uma só entrada de stock por linha |
| CD-03 | Aceites 6, danificadas 1, em falta 3 | Dívida só pelas 6 |
| CD-04 | Duas recepções parciais | Duas compras, somando o que foi aceite |
| CD-05 | Mesma chave de idempotência repetida | Nenhuma dívida nova |
| CD-06 | Dívida criada | Publica o facto contabilístico com líquido, IVA e total |
| CD-07 | Dívida criada | Nada pago |
| CD-08 | Empresa criada | Publica o facto, e o plano de contas é criado |
| CD-09 | Arranque com empresa sem plano | Só essa empresa recebe plano |
| CD-10 | Recibo de venda POS | Título "RECIBO POS" |
| CD-11 | Mesmo documento de fatura manual | Título "FATURA" |

Testes: `PurchaseOrderPayableHarnessTest`, `ChartOfAccountsBootstrapTest`, `PlatformCompanyServiceTest`
e `ReceiptTitleHarnessTest`. `scripts/empresa-exemplo.py` prova o ciclo completo na API.

## Limites

- Encomendas recebidas antes desta alteração não ganham dívida retroactiva.
- Vendas feitas sem plano de contas não são reconstruídas na contabilidade.
- Se a fatura do fornecedor trouxer outro preço, a diferença ainda não tem documento próprio.

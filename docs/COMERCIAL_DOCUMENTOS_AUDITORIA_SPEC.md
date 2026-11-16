# Faturas, notas, guias e encomendas — Auditoria

**Última actualização:** 2026-11-01

## Objectivo

Corrigir os erros que tornavam os valores, o stock, a contabilidade ou os documentos impressos
incorrectos. As decisões de negócio ainda abertas estão no fim.

## Regras corrigidas

### Fatura

- O desconto de cada linha tem de estar entre 0 e 100%.
- Anular uma fatura parcialmente paga repõe o stock, tal como nas restantes.
- Uma venda de balcão não se anula aqui: faz-se a devolução no POS, que repõe caixa e stock.
- Uma fatura com notas aprovadas não pode ser anulada: reporia o stock pela segunda vez.
- Anular uma venda ou um recibo estorna o lançamento contabilístico, trocando débitos e créditos.
- Uma fatura com desconto acima de 10% passa a chegar à contabilidade quando é aprovada.

### Saldo da fatura

- `note_adjustment` guarda o efeito das notas aprovadas: débitos somam, créditos subtraem.
- O saldo em dívida é `total + note_adjustment − recebido`. Serve recibos, antiguidade de saldos,
  limite de crédito e cobrança.
- A migração V106 calcula este valor para as notas já aprovadas.

### Notas

- A nota de crédito usa o desconto da linha da fatura: credita o que o cliente pagou.
- Só se emitem ou aprovam notas sobre faturas válidas.
- O armazém de devolução tem de ser da mesma empresa.
- Repetir a mesma linha no pedido não ultrapassa a quantidade vendida.
- Nota de débito: valor positivo e taxa de IVA abaixo de 1.
- Totais arredondados a 2 casas, como na fatura.
- Só se rejeita ou cancela uma nota ainda pendente.

### Encomenda

- Cancelar só antes de haver guia, transferência ou fatura. Antes, uma encomenda já expedida podia
  ser cancelada.
- Faturar liberta a reserva de stock em qualquer caminho.
- O autor da fatura e do recibo é o utilizador, não `SYSTEM`.
- A criação passa a validar quantidade e desconto, como a alteração já fazia.

### Fluxo guia e fatura

- Uma guia aprovada, com ou sem encomenda, fatura-se com `POST /api/comercial/delivery-guides/{id}/bill`
  ou com o botão "Faturar".
- Essa fatura não move stock: saiu na aprovação da guia. A contabilidade recebe a venda com o custo.
- A guia e a encomenda ficam ligadas à fatura, e não se fatura a mesma guia duas vezes.
- Anular a fatura não repõe stock e deixa a guia pronta a faturar de novo. Uma devolução real faz-se
  com nota de crédito, que repõe o stock.

### Separação parcial

- Depois da conferência, fatura-se e baixa-se o que foi separado. Linhas com zero ficam de fora.
- A quantidade separada não pode exceder a encomendada.
- O que ficou por separar não gera nova encomenda.

### Circuito da encomenda (revisão de 2026-11-07)

- **Faturação:** `POST /orders/{id}/bill` valida o perfil também no serviço, e não só na
  `ApiRolePolicy`. Um pedido de separação só se fatura no fim do circuito (`/fulfillment-bill`),
  com estado `INVOICED` e evento gravado.
- **Conferência:**
  - Só para pedidos de separação que aguardam ou estão em separação.
  - Tem de indicar todas as linhas, aceitando zero. Linhas em falta, repetidas ou de outra
    encomenda são recusadas.
  - A reserva passa a ser a quantidade separada.
- **Folha A4 de separação:** é `POST`, porque inicia a separação. Recusa encomendas A4.
- **Reservas (RS-01 a RS-07):** faturar uma encomenda e aprovar uma guia não levam stock reservado
  para outro pedido. A reserva da própria encomenda conta a seu favor. A verificação soma as linhas
  do mesmo artigo. A guia é bloqueada antes de ser lida.
- **Estados (EN-01 a EN-07):**
  - Os estados estão no enum `OrderStatus` em `contracts`; a coluna continua a ser texto.
  - `BILLED` e `INVOICED` contam ambos como faturada, e `INVOICED` deixou de aparecer "em atraso".
  - As pesquisas de encomendas abertas são feitas na base.
- **Aprovações:** quem submete um pedido não o aprova. O ADMIN fica de fora, para lojas com um só
  responsável. O pedido pendente é procurado por documento, não na fila inteira.
- **Armazém:** um armazém inactivo não recebe encomendas.
- **Crédito (CR-01 a CR-04):** a última aprovação de uma encomenda A4 feita por um gerente ou ADMIN
  decide o excesso de crédito, dentro das alçadas do `OrderRiskEvaluator`, e a fatura não o desfaz.
  A Green Lane (perfil `NONE`) e os pedidos de separação continuam presos ao limite. Decidido pelo
  responsável em 2026-11-07.
- **Venda sem encomenda (RS-08, RS-09):** o POS, a fatura directa, a cotação convertida e a fatura
  aprovada após desconto especial também não levam stock reservado (`SaleStockIssuer`).

### Desconto

- Numa encomenda formal, um desconto acima de 10% exige gerente, seja qual for o valor.
- No POS, o desconto não pode exceder 100%.

## Documentos

- Sai "programa certificado n.º 001/DGI/MC": a certificação não existe.
- Saem os dados bancários fixos. Passam a vir das contas bancárias de tesouraria da empresa, com
  número de conta. Sem contas, o bloco fica só com a referência.
- Estado e motivo em português.
- Total conforme o documento: `TOTAL CREDITADO`, `TOTAL DEBITADO` e `VALOR DECLARADO`.
- Totais da fatura: total ilíquido, desconto, subtotal, IVA com a base de cada taxa e total. O
  desconto e o total ilíquido só aparecem se houver desconto.
- Linhas: Emb., Cx. e % Cx. só aparecem com embalagens configuradas, a validade só com lotes, e o
  desconto só quando existe.
- Guias em A4 vertical, para as assinaturas ficarem na mesma página.
- A guia calcula o valor com o desconto: antes o total não batia com o subtotal.
- "Referência" repetia o número do documento e dá lugar à moeda.

### Texto legal da fatura (revisão de 2026-11-14)

- Uma fatura que não cobra IVA indica a norma concreta que o justifica. O texto vem da base legal
  da taxa (`TaxRate.legalBasis`) e sai como "Isento — {base legal}" no quadro de IVA. Linhas com
  motivos diferentes ficam em grupos separados. Saiu o genérico "Isento (Art. 9/10)".
- O motivo fica gravado na linha da fatura ao emitir (`invoice_lines.exemption_reason`, V135):
  reimprimir não muda o texto que o cliente recebeu. Linhas anteriores usam a base legal actual.
- Sem base legal configurada sai "Isento de IVA": o sistema nunca inventa um artigo.
- Uma taxa de IVA a 0% (isenta ou exportação) não se grava sem base legal.
- Fatura simplificada (talão): taxa ou "Isento" em cada linha, quadro de IVA por taxa com o
  motivo, e morada do cliente quando tem NUIT.
- Observações da fatura A4: "garantia conforme condições comerciais contratadas, sem prejuízo da
  garantia legal".

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| CD-01 | Nota de crédito parcial e total | Dívida baixa e fatura fica liquidada |
| CD-02 | Nota de débito em fatura paga | Cobrança reaberta |
| CD-03 | 5 unidades a 100 MT com 10% | 450 MT creditados |
| CD-04 | Nota sobre fatura anulada | Recusada |
| CD-05 | Armazém de outra empresa | Recusado |
| CD-06 | Mesma linha duas vezes | Recusada se exceder o vendido |
| CD-07 | Anulação de venda | Lançamento com débitos e créditos trocados |
| CD-08 | Fatura impressa | Sem certificação nem banco inventados, estado em português |
| CD-09 | Totais com desconto | Ilíquido, desconto e subtotal |
| GF-01 | Faturar guia aprovada | Fatura com desconto, ligada à guia e à encomenda |
| GF-02 | Guia pendente ou já faturada | Recusada |
| GF-03 | Anular fatura de guia | Guia e encomenda prontas a faturar de novo |
| GF-04 | Anular fatura de encomenda | Encomenda volta a pendente |

| LT-01 | Quadro de IVA com linhas isentas | "Isento — {base legal}" por motivo, nunca "Art. 9/10" |
| LT-02 | Isenção sem base legal | "Isento de IVA", sem artigo inventado |
| LT-03 | Base legal muda depois da emissão | A linha mantém o motivo gravado |
| LT-04 | Linha tributada / linha anterior à V135 | Sem motivo / base legal actual |
| LT-05 | Talão de fatura simplificada | Taxa por linha, motivo e morada do cliente com NUIT |
| LT-06 | Fatura A4 | Motivo da isenção e ressalva da garantia legal |
| LT-07 | Taxa de IVA a 0% sem base legal | Recusada |

Testes: `ComercialDocumentosHarnessTest`, `DeliveryGuideBillingHarnessTest` e
`InvoiceLegalTextHarnessTest`.

## Limites

- Notas de crédito antigas continuam com o valor sem desconto, e faturas antigas com desconto acima
  de 10% continuam sem lançamento. A contabilidade tem de as rever.
- A anulação de faturas anteriores também não tem estorno retroactivo.
- `GR-FT-…` é derivado do número da fatura e não tem série própria.

## Decisões em aberto

- **Pedidos de separação:** um desconto acima de 10% não pede aprovação, mas só um gerente os fatura.

- **Texto legal da fatura:** revisão técnica feita em 2026-11-14 (ver "Texto legal da fatura"). O
  jurista ainda confirma: (1) o artigo, número e alínea de cada isenção, a configurar na base legal
  das taxas; (2) se o software de faturação precisa de autorização da AT e que menção imprimir; (3) o
  nome do talão ("Fatura Simplificada", "Venda a Dinheiro") e o seu limite; (4) a lei que fixou o IVA
  em 16% (as taxas semeadas citam "DL 8/2024", por confirmar); (5) a redacção dos juros de mora.

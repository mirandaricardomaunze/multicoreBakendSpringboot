# Spec — Colunas de Linha dos Documentos Comerciais

Define o conjunto **canónico e profissional** de colunas que cada linha de produto deve mostrar nos
documentos comerciais imprimíveis (PDF). Complementa [multicore-pdf-document](../.claude/skills/),
[MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) e [CONVENTIONS.md](../CONVENTIONS.md).

## Objectivo

Que fatura, encomenda e nota de crédito apresentem cada linha de forma completa e fiscalmente legível —
identificação do artigo (código de barras + referência + descrição), validade do lote, composição
física (embalagens e percentagem da caixa) e o detalhe de preço (preço unitário, IVA, subtotal) —
através de **um único renderizador partilhado**
(`LineItemsTableRenderer`), para que todos os documentos fiquem consistentes.

## Documentos abrangidos

| Documento | Serviço | Tem linhas de produto? |
|-----------|---------|------------------------|
| Fatura | `InvoicePrintService` | ✅ |
| Encomenda | `OrderPrintService` | ✅ |
| Nota de Crédito | `CreditNotePrintService` | ✅ |
| Nota de Débito | `DebitNotePrintService` | ❌ (baseada em valor, sem linhas de artigo) |
| Guia de Remessa | `GuideRemittancePrintService` | ✅ (gerada a partir da fatura) |

## Colunas obrigatórias (por linha)

Ordem canónica, da esquerda para a direita:

1. **Cód. Barras** — `Product.barcode` (vazio se o artigo não tiver).
2. **Referência** — `Product.reference`.
3. **Descrição** — `Product.name`.
4. **Validade** — data de validade do lote da linha (`ProductBatch.expirationDate` resolvido por
   `batchNumber`); `—` quando a linha não tem lote.
5. **Qtd** — quantidade (decimal quando aplicável).
6. **Embalagens** (`Emb.`) — quantidade equivalente de embalagens comerciais:
   `quantidade ÷ unitsPerPackage`.
7. **Caixas** (`Cx.`) — quantidade equivalente de caixas, incluindo fracções:
   `quantidade ÷ (packagesPerBox × unitsPerPackage)`.
8. **% da Caixa** (`% Cx.`) — percentagem de caixa equivalente:
   `(quantidade ÷ (packagesPerBox × unitsPerPackage)) × 100`. Assim, `100%` é uma caixa e `125%`
   corresponde a uma caixa e um quarto.
9. **Preço Unit.** — `unitPrice`.
10. **IVA** — taxa de IVA da linha em percentagem.
11. **Subtotal** — valor **líquido** da linha (`quantidade × preço − desconto`, **antes de IVA**),
   calculado por `LineCalculator.net(...)` para nunca divergir da matemática de negócio.

O IVA total e o total com imposto continuam no bloco de totais do documento (`TotalsBlockRenderer`),
não por linha — evita dupla contagem visual.

## Regras

- **Fonte única de verdade visual**: as colunas, alinhamentos e formatação vivem só em
  `LineItemsTableRenderer`. Nenhum serviço de impressão formata linhas por conta própria.
- **Mapeamento partilhado**: a tradução `linha de domínio → Row` é feita por um único componente
  (`LineRowMapper`) que resolve barcode/referência/descrição e a validade do lote — sem duplicar a
  consulta ao lote em cada serviço (DRY).
- O **subtotal** é sempre líquido (antes de IVA) e coerente com `LineCalculator`.
- A **validade** só é consultada quando há `batchNumber`; caso contrário mostra `—` (sem query inútil).
- Valores monetários via `MoneyFormat`; datas em `dd/MM/yyyy`.
- **Embalagens** e **Caixas** usam até três casas decimais e **% da Caixa** até duas, removendo zeros finais.
- As colunas de embalagem são apenas apresentação derivada da composição do produto; não alteram
  quantidade fiscal, preço, desconto, IVA, subtotal ou movimento de stock.

### Exemplo de leitura

Para um produto em que `1 caixa = 12 embalagens × 6 unidades = 72 unidades`, uma linha de
`18 unidades` mostra **Embalagens = 3**, **Caixas = 0.25** e **% da Caixa = 25%**. Uma linha de
`90 unidades` mostra **Embalagens = 15**, **Caixas = 1.25** e **% da Caixa = 125%**.

### Cabeçalhos abreviados

Para as onze colunas caberem de forma legível em A4, o cabeçalho usa:
`Cód. · Ref. · Desc. · Val. · Qtd. · Emb. · Cx. · % Cx. · P. Unit. · IVA · Subt.`.
Os nomes completos continuam visíveis no ecrã de configuração das colunas.

## Guia de Remessa

`GuideRemittancePrintService.render(invoiceId)` gera a guia a partir de uma fatura: cabeçalho da
empresa, bloco de destinatário/entrega, a **mesma tabela de linhas** (11 colunas), e blocos próprios de
transporte (transportador, matrícula, data/hora de carga, nº de volumes) e de assinaturas (expedidor /
recebido em conformidade). Referência determinística `GR-<nºfatura>` — **não consome nova numeração ao
reimprimir**. Exposta em `GET /api/print/guide/{invoiceId}` e no botão "Imprimir Guia" do `ComercialPanel`.

## Não-objectivos

- Nota de Débito permanece baseada em valor (sem linhas de artigo).
- A guia não é um documento fiscal de pagamento (sem bloco de totais para liquidação); reflecte mercadoria
  expedida contra a fatura de origem.

## Critério de "pronto"

Fatura, cotação, encomenda, nota de crédito e guia mostram as 11 colunas acima, alimentadas pelos dados reais do artigo
e do lote, com subtotal líquido coerente com os totais — validado pelo
[DOCUMENT_LINE_COLUMNS_HARNESS.md](DOCUMENT_LINE_COLUMNS_HARNESS.md).

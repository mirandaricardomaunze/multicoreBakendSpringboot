# SPEC — Taxa de IVA canónica: a taxa é do artigo, não do ecrã

**Criado em:** 2026-08-06
**Camada:** domínio (`Product`) + serviços de venda + UI
**Sem migração** — nenhuma coluna nova; muda a **origem** da taxa usada no cálculo.

## 1. Problema (encontrado ao vivo, não em teoria)

O mesmo artigo era tributado de forma diferente conforme a porta por onde era vendido:

| Caminho | Farinha de Trigo (cadastrada **IVA Isento**) |
|---------|----------------------------------------------|
| POS     | 80,00 + **0,00** = 80,00 |
| Fatura  | 80,00 + **12,80** = 92,80 |

Causa: o `ComercialService` usava a taxa **enviada no pedido HTTP** (`lineReq.taxRate()`) e o
`ComercialPanel` gravava lá `TaxRates.STANDARD_VAT` fixo. O POS, esse, lia a taxa do artigo.

Consequências: o cliente paga imposto a mais em bens isentos; a **declaração mensal de IVA** e o
**SAF-T** (que lêem `invoice.taxAmount`) declaram imposto liquidado que não devia existir; a nota de
crédito herda a linha da fatura e propaga o erro.

## 2. Regra

> A taxa de IVA é uma **propriedade do artigo**. Nenhum ecrã, integração ou payload HTTP a decide.

- `Product.effectiveTaxRate()` — **fonte única**: taxa do cadastro; sem taxa, `TaxRates.STANDARD_VAT`
  (16%). Mesmo padrão do já existente `Product.effectiveUnitPrice(qty)`.
- `POSService.checkout`, `ComercialService.createInvoice` e `ComercialService.createOrder` chamam-na.
- `billOrder` e a guia de remessa herdam a linha da encomenda — passam a estar certos por
  construção, já que a encomenda deixou de divergir.
- `ProductDTO.effectiveTaxRate()` espelha a regra **só para pré-visualizar totais** nos painéis; não
  decide imposto.

## 3. Compatibilidade

`CreateInvoiceLineRequest.taxRate` continua a existir e continua `@NotNull` — não se quebra nenhum
cliente. **O valor passou a ser ignorado**: o servidor resolve sempre pelo artigo. Era precisamente
a porta que permitia a qualquer integração faturar à taxa que lhe apetecesse.

## 4. Compras: manda a factura do fornecedor

Numa **venda** a taxa é do artigo. Numa **compra** não: quem manda é o documento que o fornecedor
emitiu — o mesmo artigo pode chegar com taxas diferentes de fornecedores diferentes, e o que se
deduz é o que lá está escrito. Aplicava-se 16% cego a tudo, o que inflava o **IVA dedutível** em
bens isentos.

Regra (`PurchaseService`, `PurchaseOrderService`):

1. Taxa indicada na linha (campo **"IVA da factura (%)"** no `ComprasPanel`) → usa-se essa.
2. Campo vazio → `Product.effectiveTaxRate()`, a taxa do artigo.
3. Nunca uma constante cega.

O campo é opcional em `CreatePurchaseLineRequest` e `CreatePurchaseOrderLineRequest`, ambos com
construtor retrocompatível — nenhuma chamada existente quebra. A UI aceita a **percentagem**
(`16`, `5,5`) e converte para fracção em `ComprasPanel.parsePercentageOrNull`.

## 5. Ficheiros

| Ficheiro | Papel |
|----------|-------|
| `Product.effectiveTaxRate()` | fonte única da taxa (regra pura de domínio) |
| `ComercialService` (fatura, encomenda) | passa a resolver pelo artigo |
| `POSService` | deixa de repetir a regra; delega |
| `ProductDTO.effectiveTaxRate()` | espelho para a pré-visualização nos painéis |
| `ComercialPanel` | deixa de gravar 16% fixo nas linhas de fatura e encomenda |
| `PurchaseService`, `PurchaseOrderService` | taxa da factura do fornecedor; sem ela, a do artigo |
| `CreatePurchase(Order)LineRequest` | campo `taxRate` opcional, com construtor retrocompatível |
| `ComprasPanel` | campo "IVA da factura (%)" + coluna IVA no rascunho |

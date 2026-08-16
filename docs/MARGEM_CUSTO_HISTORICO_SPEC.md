# Margem com o custo do acto da venda — especificação

**Criado em:** 2026-08-15
**Estado:** implementado (backend), automatizado em MC-01..MC-06
**Origem:** lacuna levantada na auditoria de gestão de 2026-08-09 (`tasks/current.md`): *"margem
calculada com o preço de compra **actual** (não o do acto da venda)"*.

---

## 1. O problema

`ReportService.marginByProduct` calculava o custo assim:

```java
BigDecimal purchasePrice = line.getProduct().getPurchasePrice();   // preço de compra ACTUAL
current.estimatedCost = current.estimatedCost.add(purchasePrice.multiply(quantity));
```

O custo de uma venda passada era lido do **cadastro do produto hoje**. Consequência: bastava o
fornecedor mudar de preço — ou o operador corrigir o cadastro — para a margem de vendas já
feitas mudar sozinha, sem que nada tivesse acontecido a essas vendas.

Um exemplo do que isto faz a uma decisão de gestão: arroz vendido a 100 quando custava 60
(margem 40). O fornecedor sobe para 80. O relatório do mês passado passa a dizer margem 20. O
gestor conclui que o produto deixou de compensar — quando na verdade ganhou 40 naquela venda, e
a pergunta certa ("ainda compensa comprar a 80?") é sobre as vendas **futuras**.

É a mesma forma dos bugs já fechados neste sistema: **um número derivado de um valor que muda,
quando devia ter sido fotografado no momento do facto** — como o IVA (que passou a vir do
artigo) e o vencimento (gravado no documento, não recalculado a partir do cliente).

---

## 2. A regra canónica

### `InvoiceLine.unitCost` — fotografia, não referência
O custo unitário é **gravado na linha** no momento em que a venda se realiza. Nunca é
recalculado depois.

### `InvoiceLine.effectiveUnitCost()`
| Situação | Custo usado |
|---|---|
| linha com `unitCost` gravado | esse valor |
| linha anterior à V37 (`null`) | preço de compra actual do produto — **estimativa** |
| sem produto ou sem preço de compra | zero |

Mesmo padrão de `Product.effectiveTaxRate()` e `Invoice.effectiveDueDate()`: o recurso existe
só para os dados antigos, e está documentado como aproximação.

### `InvoiceLine.lineCost()`
`effectiveUnitCost() × quantidade`. É esta a única fonte do custo de uma linha vendida — o
relatório deixou de fazer a multiplicação por sua conta.

---

## 3. Onde a fotografia é tirada

| Porta | Momento |
|---|---|
| `ComercialService.createInvoice` | emissão da fatura |
| `ComercialService.billOrder` | **facturação** da encomenda, não a data da encomenda — é aí que a venda se realiza e o stock sai |
| `POSService.checkout` | venda no balcão |

---

## 4. Limites conhecidos (v1)

- O custo fotografado é o **preço de compra do cadastro**, não o custo médio ponderado nem o
  custo do lote FEFO efectivamente expedido. Numa loja com lotes comprados a preços diferentes,
  o custo exacto seria o do lote que saiu. Fica para v2 (implica ligar a linha ao movimento de
  stock, que já regista o lote).
- Notas de crédito (devoluções) não descontam o custo do produto devolvido no relatório de
  margem.
- Sem valorização de inventário (o stock em armazém continua sem custo associado no balanço).

---

## 5. Migração V37

```sql
alter table invoice_lines add column if not exists unit_cost numeric(14, 2);
```

**Sem backfill deliberadamente.** Escrever hoje o preço actual nas linhas antigas seria gravar
uma mentira com aspecto de facto; deixá-las nulas mantém-nas honestamente como estimativa.

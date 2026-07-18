# Spec — Preço de venda ao grosso (por produto + quantidade mínima)

> Cada produto pode ter um **2º preço (grosso)** que se aplica automaticamente quando a quantidade da
> linha atinge uma **quantidade mínima**. Retalho continua a ser o preço normal.

**Última actualização:** 2026-07-03

## Problema

A loja vende ao grosso (revendedores levam caixas), mas o sistema só tinha **um preço** por produto.
Dar desconto de grosso obrigava a mexer no preço à mão, linha a linha — inconsistente e propenso a erro.

## Decisão

- **`Product`** ganha dois campos opcionais (migração `V20`): **`wholesalePrice`** (preço ao grosso)
  e **`wholesaleMinQty`** (quantidade mínima, em unidades, a partir da qual se aplica).
- **Regra de domínio pura** `Product.effectiveUnitPrice(quantity)`: devolve `wholesalePrice` quando
  está definido, `wholesaleMinQty > 0` e `quantity ≥ wholesaleMinQty`; caso contrário `unitPrice`
  (retalho). Sem IO — testável isoladamente.
- **Aplicada nos três fluxos de venda**: `ComercialService.createInvoice`, `createOrder` e
  `POSService.checkout` passam a usar `product.effectiveUnitPrice(qty)` em vez de `getUnitPrice()`.
  O motor de cálculo (`LineCalculator`) **não muda** — recebe o preço já resolvido; IVA continua por
  unidade sobre o preço aplicado.
- **DTO/UI:** `ProductDTO` expõe `wholesalePrice`/`wholesaleMinQty`; os diálogos **Cadastrar** e
  **Editar Produto** ganham os campos "Preço Grosso (MT)" e "Qtd mín. grosso" (ambos opcionais).
- **Compatível:** produto sem preço de grosso comporta-se como antes (usa sempre retalho). Métodos
  `createProduct`/`updateProduct` mantêm as assinaturas antigas (delegam com grosso a `null`).

## Não-objetivos

- Não ligar o preço de grosso ao **cliente** (não há "cliente revendedor" nesta iteração) — o
  gatilho é a **quantidade**, não quem compra.
- Não criar tabelas de preço múltiplas (retalho/grosso/promocional) — fica como evolução futura.
- Não alterar a interação com as promoções: a promoção continua a aplicar-se como desconto % sobre o
  preço resolvido (grosso ou retalho).

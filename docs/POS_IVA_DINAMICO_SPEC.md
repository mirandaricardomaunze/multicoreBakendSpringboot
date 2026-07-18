# Spec — IVA dinâmico no POS (taxa por produto)

> O POS passa a calcular e mostrar o **IVA por linha** usando a **taxa de cada produto** (não a
> constante 0.16). Alinhado com [UI_DESIGN_SYSTEM.md](UI_DESIGN_SYSTEM.md), [CONVENTIONS.md](../CONVENTIONS.md)
> e a engine existente [LineCalculator](../src/main/java/com/phcpro/architecture/pricing/LineCalculator.java).

**Última actualização:** 2026-06-27

## Problema

1. O IVA aplicado era **estático**: `TaxRates.STANDARD_VAT = 0.16` hardcoded em todas as linhas
   (POS, faturação, compras, CRM).
2. O carrinho do POS **não mostrava IVA** — só o líquido. O operador e o cliente não viam a carga
   fiscal nem o total real a pagar.
3. Em Moçambique a cesta básica (açúcar, arroz, farinha…) é **isenta**; tributá-la a 16% está errado.

## Decisão

- **Fonte da taxa = o produto** (decisão do utilizador). Novo FK `Product.taxRate → TaxRate`
  (entidade fiscal configurável já existente: IVA16/IVA5/IVA0/IVAEXMT). Migration `V16`.
- **Resolução da taxa efetiva:** `product.taxRate.rate` quando definida; caso contrário a
  **taxa-padrão** (`TaxRates.STANDARD_VAT`, 16%) — documentada como default, não como regra fixa.
- **Cálculo continua na engine única** `LineCalculator.compute(unitPrice, qty, desconto, taxa)`
  (líquido → IVA → total). Sem duplicar matemática no painel.
- **Checkout** (`POSService`) grava `InvoiceLine.taxRate` com a taxa do produto. O recibo térmico,
  que já imprime IVA por linha, passa a refletir a taxa real automaticamente.
- **POS UI:** o carrinho ganha colunas **Líquido · IVA · Total** (IVA mostra "Isento" a 0% ou
  "valor (taxa%)"); abaixo, discriminação **Subtotal s/ IVA · IVA** e a faixa **TOTAL A PAGAR**
  passa a ser **líquido + IVA** (o que o cliente paga). O pagamento/troco usa este total c/ IVA.
- **Cadastro de produto:** o formulário ganha seletor **Taxa de IVA** (default IVA Normal 16%).
- **`unitPrice` é tax-exclusive** (líquido). O IVA é somado por cima — coerente com a faturação.

## Não-objetivos

- Não migrar faturação/compras/CRM para IVA por produto nesta iteração (continuam na taxa-padrão);
  a resolução fica pronta para eles adotarem depois.
- Não alterar a entidade `TaxRate` nem o ecrã de configuração fiscal.
- Não implementar preços com IVA incluído (tax-inclusive).

## Modelo

```
Product ──(ManyToOne, opcional)──▶ TaxRate (code, rate, type, active, por empresa)
linha.taxRate = product.taxRate?.rate ?? TaxRates.STANDARD_VAT
LineCalculator: net = preço·qtd − desconto ; iva = net·taxa ; total = net + iva
```

## Dados de demonstração (DataLoader)

Cesta básica isenta (Arroz, Açúcar, Farinha, Feijão), Massa a IVA 5%, Óleo a IVA 16% — para o POS
mostrar taxas variadas numa só venda.
</content>

# Balança / venda ao peso — código de barras de medida variável

**Última actualização:** 2026-07-11
**Estado:** feito (parser + integração POS + testes). Hardware real por validar em loja.

## Objectivo

Fechar a última lacuna de loja: vender artigos **ao peso** (carne, queijo, fruta pesada ao balcão)
lendo a **etiqueta que a balança imprime**. Essa etiqueta não é um EAN de fabricante — é um
**código de barras de medida variável** que traz embutido o **artigo (PLU)** e a **medida** (peso ou
preço). O POS passa a interpretá-la, resolver o produto e adicionar ao carrinho já com o peso lido.

Complementa a Fase 3 do retalho (que já introduziu `ProductSaleType.WEIGHT`, `stockTracked` e
**quantidades decimais** em todo o fluxo). Faltava apenas **ler a etiqueta** — é o que isto entrega.

## Formato da etiqueta (EAN-13, configurável)

```
  [ prefixo ][ artigo (PLU) ][ medida ][ dígito de controlo ]   → 13 dígitos
      2          00042          001500          0
```

Configurável em `retail.scale.*` (`application.properties`); defaults = caso comum:

| Propriedade                   | Default  | Significado                                            |
|-------------------------------|----------|--------------------------------------------------------|
| `retail.scale.enabled`        | `true`   | Desligar = tudo tratado como código de barras normal.  |
| `retail.scale.prefix`         | `2`      | Dígito(s) que marcam etiqueta de medida variável.      |
| `retail.scale.item-digits`    | `5`      | Nº de dígitos do PLU.                                   |
| `retail.scale.measure-digits` | `6`      | Nº de dígitos da medida.                                |
| `retail.scale.embedded`       | `WEIGHT` | `WEIGHT` (peso em gramas) ou `PRICE` (preço em cêntimos). |
| `retail.scale.weight-divisor` | `1000`   | Medida ÷ divisor = quilos (gramas → kg).               |
| `retail.scale.price-divisor`  | `100`    | Medida ÷ divisor = meticais (cêntimos → MT).           |

**Regra de validade:** `prefixo + PLU + medida + 1 (controlo)` tem de dar **13**. Se não der (ou se
`enabled=false`), o parser ignora tudo — **falha segura**: nenhuma leitura é tratada como balança.

## Fluxo

1. **Leitura** (`ScaleBarcodeParser.parse`): syntax only → `ScaleBarcode(itemCode, measure)`, ou vazio
   se não for etiqueta de balança (segue o caminho normal de código de barras).
2. **Resolução do artigo**: o **PLU** mapeia para o campo **"Código de barras" do produto** (o operador
   regista o PLU da balança aí). Tenta o código tal-e-qual e depois sem zeros à esquerda.
3. **Quantidade (kg)**:
   - `WEIGHT` → `kg = medida ÷ weight-divisor`.
   - `PRICE`  → deriva o peso: `kg = (medida ÷ price-divisor) ÷ preço/kg` (o total volta a bater certo).
4. **Adiciona ao carrinho** (`addWeighedProductToCart`): merge com linha existente do mesmo artigo
   (soma o peso), melhor promoção para a quantidade, cálculo de dinheiro pela engine
   (`LineCalculator`: preço/kg × kg, IVA por unidade) — como qualquer outra linha.
5. **Checkout/stock**: a quantidade decimal já flui até ao `StockMovement SALE` (Fase 3). Sem alterações
   ao domínio.

## Peças

- **`modules/pos/scale/`** — `EmbeddedMeasure`, `ScaleBarcode` (record), `ScaleBarcodeFormat`,
  `ScaleBarcodeParser` (lógica **pura**, sem Spring/IO), `ScaleConfig` (@Configuration → bean a partir
  de `retail.scale.*`).
- **`POSPanel`** — `handleBarcodeScan` tenta balança primeiro; `handleScaleScan` +
  `resolveWeighedProduct` + `addWeighedProductToCart`. Wiring do parser via `MainFrame`.

## Guardas / mensagens

- PLU não encontrado → aviso "registe o PLU no campo Código de barras do produto".
- Artigo não é do tipo **Peso** → aviso "defina Tipo de Venda = Peso".
- `PRICE` sem preço/kg, ou peso zero → aviso, não adiciona.

## Notas / limites

- **Sem migração de BD**: reutiliza o campo `barcode` do produto como PLU. Um campo `plu` dedicado
  (único por empresa) é um passo futuro se se quiser separar PLU de EAN.
- **Sem UI de configuração por empresa** nesta iteração — o formato é global via `application.properties`.
  Config por empresa (como os documentos) é um passo futuro.
- **Dígito de controlo** não é validado (algoritmos de balança variam); a medida é lida por posição.
- **Hardware real** (balança + impressora de etiquetas) por validar em loja — ver harness BV-50+.

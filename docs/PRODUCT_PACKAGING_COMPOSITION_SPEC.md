# Spec — Composição Caixa → Embalagem → Unidade

## Objectivo

Permitir que cada produto descreva a sua embalagem comercial completa:

```text
1 caixa = embalagens por caixa × unidades por embalagem
```

Exemplo: `1 caixa = 12 embalagens × 6 unidades = 72 unidades`.

## Regras

- O stock, preços, impostos e movimentos continuam expressos na unidade-base.
- `packagesPerBox` e `unitsPerPackage` são inteiros positivos.
- `unitsPerBox` permanece no contrato e na base de dados como total derivado e compatível:
  `unitsPerBox = packagesPerBox × unitsPerPackage`.
- O backend é a fonte de verdade do total; o cliente nunca consegue persistir uma composição
  incoerente.
- Produtos anteriores são migrados sem alterar o stock: cada unidade antiga passa a representar
  uma embalagem de uma unidade (`packagesPerBox = unitsPerBox`, `unitsPerPackage = 1`).
- Entradas e documentos podem informar caixas completas, embalagens soltas e unidades soltas.
- Embalagens soltas devem ser inferiores a uma caixa; unidades soltas devem ser inferiores a uma
  embalagem.
- A decomposição é apenas uma camada de entrada e apresentação. A quantidade fiscal e de stock
  continua a ser o total de unidades.

## Compatibilidade HTTP

- `ProductDTO` expõe `packagesPerBox`, `unitsPerPackage` e o `unitsPerBox` derivado.
- `CreateProductRequest` mantém `unitsPerBox` para clientes anteriores e aceita os dois novos
  campos opcionais.
- Quando os novos campos não são enviados, o backend adopta a composição compatível
  `unitsPerBox × 1`.

## Interface

O cadastro e a edição apresentam:

- Embalagens por caixa;
- Unidades por embalagem;
- Total de unidades por caixa, calculado e não editável.

Os editores de quantidade apresentam Total, Caixas, Embalagens e Unidades.

## Colunas explicativas nos documentos comerciais

Os documentos que justificam o movimento de mercadoria — factura, cotação, encomenda, nota de
crédito e guia — apresentam três colunas derivadas ao lado da quantidade:

- **Embalagens**: quantidade da linha convertida em embalagens equivalentes.
  Fórmula: `quantidade em unidades ÷ unitsPerPackage`.
- **Caixas**: quantidade da linha convertida em caixas equivalentes, incluindo fracções.
  Fórmula: `quantidade em unidades ÷ unitsPerBox`.
- **% da Caixa**: proporção de uma caixa completa representada pela quantidade da linha.
  Fórmula: `(quantidade em unidades ÷ unitsPerBox) × 100`.

Exemplo para `12 embalagens/caixa × 6 unidades/embalagem = 72 unidades/caixa`:

| Quantidade da linha | Embalagens | Caixas | % da Caixa | Significado |
|--------------------:|-----------:|-------:|------------:|-------------|
| 18 unidades | 3 | 0.25 | 25% | Um quarto de caixa |
| 72 unidades | 12 | 1 | 100% | Uma caixa completa |
| 90 unidades | 15 | 1.25 | 125% | Uma caixa e um quarto |

As três colunas são informativas. A quantidade fiscal, o preço unitário, o IVA, o subtotal e o
movimento de stock continuam calculados exclusivamente em unidades-base. A configuração do produto
é a fonte dos factores; o operador não introduz percentagens manualmente.

# Selecção Pesquisável de Produtos

## Problema

Os formulários operacionais não podem depender de percorrer manualmente um select com todo o
catálogo. Com muitos artigos, essa abordagem aumenta o tempo de emissão e facilita escolher o
produto errado.

## Comportamento canónico

- O select de produto é editável e filtra enquanto o utilizador escreve.
- A pesquisa considera nome, SKU, referência, código de barras, descrição e categoria.
- A pesquisa ignora diferenças entre maiúsculas, minúsculas e acentos e aceita vários termos.
- O resultado apresenta código, código de barras quando existir, nome e preço de venda.
- O valor seleccionado é o próprio `ProductDTO`; a regra não depende da posição do produto numa
  lista paralela.
- O controlo mantém a altura canónica de 38 px e suporta rato e teclado.
- Catálogos vazios ou pesquisas sem resultado deixam a selecção vazia e impedem adicionar a linha.

## Aplicação

O componente reutilizável `ProductSearchComboBox` é usado nos formulários de factura, pedido de
cliente, compra, encomenda a fornecedor, cotação, promoção, transferência, ajuste, entrada de lote
e edição de produto. Selects tipados auxiliares, como o registo de quebras, usam a base genérica
`SearchableComboBox` sem duplicar o algoritmo de filtragem.

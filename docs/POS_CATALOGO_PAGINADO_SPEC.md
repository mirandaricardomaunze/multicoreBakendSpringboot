# Spec — Paginação do catálogo POS

## Objectivo

Evitar carregar todos os produtos e respectivas imagens ao abrir o POS. Pesquisa, disponibilidade e
paginação passam a ser resolvidas no servidor antes da transferência para o desktop.

## Contrato

`GET /api/comercial/products/pos-catalog/page`

Parâmetros: `query`, `availableOnly`, `page` (base zero) e `size` (36 no desktop). A resposta é
`PageResponse<POSCatalogItemDTO>`, onde cada item contém `ProductDTO product` e `boolean sellable`.

`GET /api/comercial/products/pos-catalog/by-barcode` preserva a leitura de produtos que não estejam
na página visível e devolve o mesmo estado vendável.

## Interface

- 36 produtos por página.
- Rodapé: **Anterior**, `Página X de Y · N produtos`, **Próximo**.
- Botões desactivados nos limites.
- Pesquisa tem debounce de 300 ms e volta à primeira página.
- Alterar Todos/Disponíveis volta à primeira página.
- Respostas antigas de pesquisas concorrentes são ignoradas por sequência de pedido.
- Chamadas HTTP permanecem fora do EDT.

## Segurança operacional

O servidor continua a calcular a disponibilidade através de `InventoryService`. O checkout conserva
a validação concorrente final de stock.

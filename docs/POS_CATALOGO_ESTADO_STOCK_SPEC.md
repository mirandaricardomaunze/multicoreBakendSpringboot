# Spec — Estado de stock no catálogo POS

## Objectivo

Permitir que o operador encontre produtos esgotados sem permitir a sua venda. O desaparecimento de
um produto deixa de ser usado como indicador de indisponibilidade.

## Comportamento

- O catálogo abre no filtro **Todos** e oferece **Disponíveis** na mesma linha da pesquisa.
- O desktop carrega todos os produtos e a lista canónica de produtos vendáveis do backend.
- Produtos esgotados aparecem com fundo e texto atenuados, cursor normal e etiqueta **ESGOTADO**.
- O tooltip explica que não existe stock disponível para venda.
- Produtos esgotados não respondem ao clique.
- Clique programático, código de barras e etiqueta de balança têm a mesma protecção visual.
- O checkout continua a executar a validação oficial e concorrente de stock no backend.

## Fonte da disponibilidade

A interface não calcula stock. Um produto é considerado disponível quando o seu ID consta no retorno
de `/api/comercial/products/sellable`, cuja regra pertence a `ComercialService` e `InventoryService`.

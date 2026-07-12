# Harness — Etiquetas de código de barras

Ver [ETIQUETAS_CODIGO_BARRAS_SPEC.md](ETIQUETAS_CODIGO_BARRAS_SPEC.md).

## Manuais

| ID     | Cenário                                                                 | Esperado                                                        |
|--------|-------------------------------------------------------------------------|----------------------------------------------------------------|
| ET-50  | Stock → Etiquetas → selecionar 3 produtos, 2 cópias, Imprimir           | PDF A4 com 6 etiquetas (3 por linha); cada uma com nome+código+preço. |
| ET-51  | Produto com `barcode` EAN                                               | Etiqueta usa o EAN; texto legível por baixo bate certo.        |
| ET-52  | Produto sem `barcode` nem `reference`                                   | Usa o SKU como código.                                          |
| ET-53  | Ler a etiqueta impressa com um leitor real                             | Scanner devolve o código impresso (Code128).                   |
| ET-54  | Nenhum produto selecionado                                              | Aviso "selecione pelo menos um produto".                       |
| ET-55  | Cópias fora de 1..200                                                   | Aviso / limita ao intervalo.                                   |

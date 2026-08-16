# Harness — Estado de stock no catálogo POS

| Caso | Acção | Resultado esperado |
|---|---|---|
| ES-01 | Abrir POS com produto disponível e produto esgotado | Ambos aparecem no filtro Todos. |
| ES-02 | Observar produto esgotado | Cartão atenuado, etiqueta ESGOTADO e cursor sem acção. |
| ES-03 | Clicar no produto esgotado | Nenhuma linha é adicionada ao carrinho. |
| ES-04 | Seleccionar filtro Disponíveis | Produtos esgotados desaparecem; vendáveis permanecem. |
| ES-05 | Voltar ao filtro Todos | Produtos esgotados tornam a aparecer. |
| ES-06 | Ler código de barras de produto esgotado | Aviso Sem Stock; carrinho não muda. |
| ES-07 | Repor stock e recarregar o POS | Produto perde o estado ESGOTADO e torna-se clicável. |

## Evidência automatizada

- `PosCatalogAvailabilityTest` cobre os filtros Todos e Disponíveis.
- A suite completa cobre a validação oficial de stock no checkout.

# Harness — Paginação do catálogo POS

| Caso | Acção | Resultado esperado |
|---|---|---|
| PG-01 | Abrir POS com mais de 36 produtos | Mostra no máximo 36 cartões e Página 1 de N. |
| PG-02 | Clicar Próximo | Carrega a página seguinte sem bloquear a janela. |
| PG-03 | Clicar Anterior na segunda página | Regressa à primeira; Anterior fica desactivado. |
| PG-04 | Pesquisar por nome/SKU/referência/barcode | Resultado e totais de página reflectem a pesquisa do servidor. |
| PG-05 | Alternar Todos/Disponíveis | Volta à página 1 e recalcula o total. |
| PG-06 | Digitar rapidamente | Apenas a pesquisa final é aplicada após 300 ms. |
| PG-07 | Ler barcode de produto noutra página | Produto é encontrado directamente no servidor e adicionado se disponível. |
| PG-08 | Ler barcode de esgotado | Aviso Sem Stock; carrinho não muda. |

## Regressão corrigida

O harness inclui agora `ComercialControllerIntegrationTest.authenticatedDesktopCanLoadPaginatedPOSCatalog`,
que executa o endpoint HTTP autenticado com empresa activa e valida a estrutura completa da página.

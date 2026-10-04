# Harness — Selecção Pesquisável de Produtos

1. Carregar produtos com nome, SKU, referência, código de barras e categoria distintos.
2. Pesquisar por cada identificador e confirmar que apenas os produtos correspondentes aparecem.
3. Pesquisar sem acentos e confirmar correspondência com nomes acentuados.
4. Pesquisar por dois termos e confirmar que ambos são exigidos.
5. Seleccionar o resultado e confirmar que o formulário recebe o `ProductDTO` correcto.
6. Limpar a pesquisa e confirmar que o catálogo completo volta a aparecer.
7. Executar `DesktopThinContextTest` para confirmar a instanciação do `MainFrame`.

## Automação

O teste `ProductSearchComboBoxHarnessTest` cobre nome, SKU, referência, código de barras,
descrição, categoria, pesquisa sem acentos, pesquisa com vários termos, selecção tipada por ID,
pesquisa sem resultado, reposição do catálogo, conteúdo do rótulo e altura canónica de 38 px.

Executar o contrato completo com:

```powershell
mvn -q "-Dtest=ProductSearchComboBoxHarnessTest,DesktopThinContextTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

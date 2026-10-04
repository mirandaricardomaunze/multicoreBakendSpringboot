# Quantidades por Caixa, Embalagem e Unidade

- Cada produto define `packagesPerBox` e `unitsPerPackage`; `unitsPerBox` é o produto derivado.
- O operador pode introduzir caixas, embalagens e unidades soltas; o total é calculado imediatamente.
- Ao alterar o total, os três níveis são recalculados por divisão inteira.
- Embalagens soltas devem ficar entre zero e `packagesPerBox - 1`.
- Unidades soltas devem ficar entre zero e `unitsPerPackage - 1`.
- Total monetario e stock usam sempre a quantidade total, evitando duas fontes de verdade.
- Os editores apresentam a decomposição `N cx + M emb + P un` sem alterar a quantidade fiscal.
- O formulário inicia e volta a `0` após adicionar uma linha, evitando uma unidade solta residual.
- O produto seleccionado mostra explicitamente o factor, por exemplo `Qtd total (12 un/caixa)`.
- Exemplo: 12 embalagens/caixa × 6 unidades/embalagem = 72 unidades/caixa.
- Exemplo: 2 caixas + 3 embalagens + 4 unidades = 166 unidades.
- O editor canónico `PackageQuantityEditor` é reutilizado em faturas, pedidos de cliente, compras
  directas e encomendas a fornecedor.
- Guias herdam a quantidade total da origem e apresentam a decomposição sem permitir divergência.
- Documentos comerciais mostram **Embalagens** (`total ÷ unitsPerPackage`), **Caixas**
  (`total ÷ unitsPerBox`) e **% da Caixa** (`total ÷ unitsPerBox × 100`) para justificar visualmente
  a quantidade, sem alterar os cálculos.
- Novos documentos com linhas de produto, incluindo futuras cotações, devem reutilizar o mesmo editor.

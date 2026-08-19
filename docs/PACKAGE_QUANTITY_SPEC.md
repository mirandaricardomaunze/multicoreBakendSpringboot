# Quantidades por Caixa e Unidade

- Cada produto usa `unitsPerBox` como factor canonico.
- O operador pode introduzir caixas e unidades soltas; o total e calculado imediatamente.
- Ao alterar o total, caixas e unidades soltas sao recalculadas por divisao inteira.
- Unidades soltas devem ficar entre zero e `unitsPerBox - 1`.
- Total monetario e stock usam sempre a quantidade total, evitando duas fontes de verdade.
- Pedidos e guias apresentam a decomposicao `N cx + M un` sem alterar a quantidade fiscal.
- O formulário inicia e volta a `0` após adicionar uma linha, evitando uma unidade solta residual.
- O produto seleccionado mostra explicitamente o factor, por exemplo `Qtd total (12 un/caixa)`.
- Exemplo: produto com 12 unidades por caixa + 2 caixas + 0 soltas = 24 unidades.
- Exemplo inverso: 29 unidades = 2 caixas completas + 5 unidades soltas.
- O editor canónico `PackageQuantityEditor` é reutilizado em faturas, pedidos de cliente, compras
  directas e encomendas a fornecedor.
- Guias herdam a quantidade total da origem e apresentam a decomposição sem permitir divergência.
- Novos documentos com linhas de produto, incluindo futuras cotações, devem reutilizar o mesmo editor.

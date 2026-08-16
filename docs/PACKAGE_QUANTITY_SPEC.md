# Quantidades por Caixa e Unidade

- Cada produto usa `unitsPerBox` como factor canonico.
- O operador pode introduzir caixas e unidades soltas; o total e calculado imediatamente.
- Ao alterar o total, caixas e unidades soltas sao recalculadas por divisao inteira.
- Unidades soltas devem ficar entre zero e `unitsPerBox - 1`.
- Total monetario e stock usam sempre a quantidade total, evitando duas fontes de verdade.
- Pedidos e guias apresentam a decomposicao `N cx + M un` sem alterar a quantidade fiscal.

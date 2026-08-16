# Harness — Carrinho operacional do POS

## Cores das acções

| Caso | Verificação | Esperado |
|---|---|---|
| PC-40 | Abrir caixa / Finalizar venda | verde |
| PC-41 | Quantidade e Novo cliente | azul |
| PC-42 | Sangria / Suprimento | âmbar |
| PC-43 | Fechar caixa / Remover | vermelho |
| PC-44 | Alternador de vista inactivo | grafite visível, sem preto puro |

| Caso | Acção | Resultado esperado |
|---|---|---|
| CO-01 | Abrir caixa e clicar num produto | O carrinho substitui o estado vazio, selecciona a nova linha e mostra-a no viewport. |
| CO-02 | Adicionar vários produtos até ultrapassar a altura | A última linha adicionada fica visível automaticamente. |
| CO-03 | Clicar novamente num produto existente | A quantidade aumenta na mesma linha e a linha fica seleccionada. |
| CO-04 | Observar o carrinho a 1366×768 | Artigo, Qtd, Preço, Desc., IVA e Total aparecem sem scroll horizontal. |
| CO-05 | Passar o rato sobre uma linha | Tooltip mostra promoção e, quando aplicável, lote/série. |
| CO-06 | Seleccionar linha e usar + / − | Quantidade e totais actualizam; − em quantidade 1 remove a linha. |
| CO-07 | Premir F6 numa linha | Permite quantidade exacta, incluindo decimal. |
| CO-08 | Finalizar venda | Fluxo de pagamento, stock, fiscalidade e limpeza do carrinho permanecem intactos. |
| CO-09 | Reduzir a janela abaixo do viewport confortável | Todas as seis colunas permanecem visíveis; nenhuma fica cortada sem acesso. |
| CO-10 | Clicar em Mais opções | Abre diálogo de armazém/tesouraria; o cabeçalho e o carrinho não mudam de posição. |
| CO-11 | Abrir o POS numa janela com pouca altura | Cabeçalho completo e pelo menos duas linhas do carrinho ficam visíveis acima do resumo. |
| CO-12 | Comparar acções de caixa e inputs | Os inputs ficam próximos dos botões, sem faixa vazia excessiva; a altura ganha pertence ao carrinho. |
| CO-13 | Observar o cabeçalho operacional | Pesquisa, Cliente, Armazém, Conta e Código de barras aparecem alinhados numa única linha. |

## Evidência automatizada

- `PosLayoutTest`: seis colunas e larguras operacionais; comportamento responsivo aos 620 px.
- Compilação completa e suite Maven devem permanecer verdes.

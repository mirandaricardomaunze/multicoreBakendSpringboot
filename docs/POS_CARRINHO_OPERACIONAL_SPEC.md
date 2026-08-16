# Spec — Carrinho operacional do POS

## Hierarquia cromática das acções

- Verde: abrir caixa e finalizar venda.
- Azul: novo cliente e ajuste de quantidade (`−`, editar e `+`).
- Âmbar: sangria/suprimento, por exigir atenção sem ser uma confirmação ou destruição.
- Vermelho: fechar caixa e remover linha.
- Grafite visível: vistas inactivas e acções estritamente neutras; não usar preto puro.

As cores comunicam significado e não decoração: acções equivalentes mantêm a mesma cor, hover e
contraste através das fábricas de `UIHelper`.

## Objectivo

Garantir que o artigo adicionado fica imediatamente visível e que as operações mais frequentes do
caixa podem ser feitas sem rolagem horizontal nem abertura obrigatória de diálogos.

## Decisões de design

- O carrinho apresenta seis colunas essenciais: **Artigo, Qtd, Preço, Desc., IVA e Total**.
- Lote, série e promoção continuam disponíveis como detalhe contextual no tooltip da linha.
- As linhas têm 42 px para leitura rápida e selecção segura.
- A linha adicionada ou incrementada é seleccionada e deslocada automaticamente para a área visível.
- Uma barra fixa mostra o total de artigos e oferece **diminuir, editar e aumentar quantidade**.
- Diminuir uma quantidade igual a um remove a linha; quantidades decimais continuam editáveis por F6.
- Subtotal, IVA, total, fiado e finalização permanecem fixos fora do scroll da tabela.
- Scanner, promoções, FEFO, stock, pagamento e cálculos oficiais não são alterados.

## Responsividade

O conjunto de colunas foi dimensionado para o viewport operacional de 620 px. A partir dessa largura,
a tabela distribui o espaço disponível e não apresenta scroll horizontal. Abaixo dessa largura,
continua a adaptar todas as colunas em vez de cortar as colunas finais.

As opções de armazém e tesouraria abrem num diálogo compacto. O cabeçalho do POS não muda de
altura e, por isso, não comprime nem sobrepõe o catálogo ou o carrinho.

O selector de cliente usa pesquisa e selecção na mesma linha. Subtotal, IVA e total partilham uma
única faixa compacta; a opção Fiado fica na linha das acções. Esta distribuição devolve mais de uma
centena de pixels verticais ao corpo da tabela nas janelas baixas.

O ritmo vertical do POS usa margem externa de 14 px, separações de secção de 6 px e intervalos do
cartão de 8 px. Os inputs ficam visualmente próximos das acções de caixa e a altura recuperada é
entregue ao catálogo/carrinho, sem reduzir a altura canónica dos controlos.

## Acessibilidade e teclado

- F6 continua a abrir a edição exacta da quantidade.
- F9 continua a finalizar a venda.
- Tooltips explicam os botões compactos e preservam os detalhes secundários da linha.

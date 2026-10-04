# Posicionamento Canónico de Acções em Tabelas

## Problema

Os módulos não podem alternar arbitrariamente os botões de uma lista entre o topo e o rodapé da
tabela. Essa variação obriga o utilizador a procurar as acções em cada ecrã e reduz a velocidade
de operação.

## Estrutura canónica

Cada lista operacional segue esta ordem visual:

1. Cabeçalho da página: título e, quando necessário, até três acções globais que não dependem da
   linha seleccionada, como criar um novo registo ou abrir outro fluxo.
2. Topo do card da tabela (`BorderLayout.NORTH`): pesquisa, filtros e acções da tabela. Em listas
   densas pode usar duas filas dentro da mesma zona: título e acções na primeira; pesquisa e filtros
   na segunda. Nenhum destes controlos fica fora do card. Para esta composição usar
   `UIHelper.tableCardTop(titulo, filtros, accoes...)`.
3. Corpo do card (`BorderLayout.CENTER`): tabela e respectivo scroll.
4. Rodapé do card (`BorderLayout.SOUTH`): somente paginação, contagem, totais ou estado da
   selecção. Não contém botões de acção.

## Densidade e ordem

- Todos os controlos da barra têm altura canónica de 38 px.
- A pesquisa precede filtros de estado, tipo e período.
- À direita, acções secundárias precedem a acção primária.
- Podem existir no máximo três botões ou menus visíveis na mesma barra.
- Acções adicionais são agrupadas em `ActionMenuButton`, com no máximo cinco opções por menu.
- Acções destrutivas usam o estilo de perigo e confirmação quando houver perda de dados.
- Paginação servida pelo backend mantém a mesma hierarquia da paginação local: tamanho e contagem à
  esquerda, página actual ao centro e navegação à direita.

## Cobertura transversal

O padrão aplica-se às listagens operacionais de Comercial, Compras, Stock, Fiscal, CRM, Recursos
Humanos, Plataforma, Notificações e Utilizadores. Cabeçalhos fora do card só são aceites quando
representam a página inteira e não acções da tabela, como dashboards, KPIs e navegação por abas.

## Excepções permitidas

- Rodapé de `ModernFormDialog` e outros diálogos: `Cancelar`, `Guardar` ou `Confirmar`.
- Fluxos sequenciais como POS, checkout e assistentes: a acção final pode permanecer embaixo.
- Tabelas editáveis de linhas seguem a mesma regra no seu próprio card: adicionar/remover no topo;
  totais no rodapé.
- Paginação, contagens, totais e mensagens informativas permanecem no rodapé e nunca são tratados
  como acções.

## Critério de pronto

- Nenhuma tabela de listagem tem botões no `BorderLayout.SOUTH` do seu card.
- Filtros e acções continuam dentro do mesmo `ModernPanel` que contém a tabela.
- Barras densas respeitam o limite de três controlos visíveis.
- `TableActionPlacementHarnessTest` e `DesktopThinContextTest` passam.

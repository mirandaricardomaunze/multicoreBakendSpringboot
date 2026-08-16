# Spec — Paginação uniforme das tabelas

## Objectivo

Todas as **tabelas de listagem** do desktop devem apresentar paginação previsível, acessível e
coerente. A paginação limita o volume visual, preserva filtros e mantém as operações remotas fora
do EDT.

## Âmbito

- Listagens carregadas integralmente: paginação local automática em 25, 50, 100 ou 200 registos.
- Listagens de grande crescimento: paginação no servidor por `PageResponse`, através de
  `TablePager` (faturas, vendas POS, diário contabilístico e catálogo POS).
- Tabelas de linhas de documento, carrinho, formulários, contagens e diálogos: **não paginadas**;
  são uma única unidade transaccional e devem permanecer integralmente visíveis/roláveis.

## Regras funcionais

1. O tamanho inicial das listagens locais é 50 registos.
2. A barra mostra página actual, total de páginas e total de registos após filtros.
3. Pesquisa, estado e período são aplicados antes da divisão em páginas e regressam à página 1.
4. Primeira/anterior/seguinte/última respeitam os limites e possuem nome acessível.
5. Alterações do modelo recalculam páginas e impedem uma página vazia fora do novo limite.
6. Uma listagem paginada no servidor não recebe uma segunda paginação local.
7. A adopção é central em `UIHelper`, evitando implementações divergentes por painel.
8. Os controlos da paginação mantêm 8 px entre si e a paginação remota reserva 10 px antes da
   fila de botões de acção colocada por baixo.

## Arquitectura

- `ClientTablePagination`: pagina listas já disponíveis no cliente e compõe o seu filtro com
  `TableFilter`.
- `TablePager`: navega páginas do backend sem conhecer HTTP ou regras de domínio.
- `PageResponse<T>`: contrato canónico das páginas remotas.
- `UIHelper.styleTable/styleScrollPane`: instala o comportamento transversal apenas em listagens
  reconhecidas por `RowSorter` e por um contentor com rodapé livre.

## Critérios de aceitação

- Nenhuma listagem sem rodapé próprio apresenta mais de 50 linhas na primeira página.
- Uma pesquisa que devolva sete registos mostra os sete, independentemente da página anterior.
- Listagens vazias mostram “Sem registos”.
- O build compila e o harness automatizado fica verde.

# Consistência das tabelas: botões, estados, rodapé e botão de espreitar

**Código:** SPEC-TABELAS-CONSISTENCIA  
**Módulo:** `desktop` (`mz.multicore.erp.gui.components`)  
**Data:** 2026-11-14  
**Estado:** APROVADO / IMPLEMENTADO  
**Harness:** `desktop/src/test/java/mz/multicore/erp/gui/components/TableConsistencyHarnessTest.java`  
**Relacionado:** `docs/ECRA_CAMINHO_E_MENU_SPEC.md` (cabeçalho comum)

## 1. Problema

Comparando Cotações, Faturação, Pedidos e Recibos: 3 ou 4 botões grandes de cores diferentes em cada
cabeçalho, com “Anular Recibo” num bloco vermelho ao lado de “Actualizar”. Havia também dois renderizadores de estado com listas de cores diferentes, dois
paginadores com textos e ícones diferentes, e o botão “Espreitar detalhes” num sítio diferente em cada ecrã.

## 2. Regras (todas centrais; os ecrãs não mudam)

1. **Hierarquia dos botões** (`TableHeaderActions`, aplicada pelo cabeçalho comum).
   - Uma só acção principal com cor cheia (verde): o último botão verde que o ecrã dá.
   - Todos os outros botões com contorno (`ButtonStyles.outline`): Actualizar, menus, azuis, amarelos e
     verdes que não são o último.
   - Botões destrutivos (vermelhos) ficam no sítio onde o ecrã os pôs, com contorno e texto e ícone a
     vermelho (`ButtonStyles.outlineDanger`). Nunca aparecem como bloco vermelho cheio. Não vão para um menu porque
     alguns são de uso constante (“Remover linha” nos editores).
2. **Um só vocabulário de estados** (`StatusTone`). A mesma lista de estados e cores é usada pelo
   renderizador comum e por `TableCellRenderers.status()`: “Paga”, “Faturado” e “Cancelado” têm a mesma cor
   em todas as tabelas. O estado continua a ser **texto colorido, não etiqueta**, por decisão de
   2026-11-05 (`TableStatusPtMzHarnessTest`). A linha só é pintada quando precisa de atenção (vermelho ou
   âmbar).
3. **Rodapé igual em todas as tabelas.** “0 registos / 1 registo / 12 registos” e “Página 1 de 1”,
   também quando a tabela está vazia, nos dois paginadores (`ClientTablePagination`, `TablePager`),
   com os mesmos ícones.
4. **Botão de espreitar no fim dos filtros.** O cabeçalho comum encontra o botão “Espreitar…” onde quer
   que o ecrã o tenha posto (filtros ou acções) e coloca-o no fim dos filtros, com contorno.

5. **Atalhos iguais em todos os ecrãs** (`TableHeaderShortcuts`). Ctrl+N corre a acção principal (o botão
   verde), F5 o Actualizar e Ctrl+F leva à pesquisa dos filtros. Ficam ligados ao cabeçalho com
   `WHEN_IN_FOCUSED_WINDOW`, por isso só o ecrã à vista responde, e só quando o botão está visível e
   activo. Atalhos mais próximos do foco (POS, pesquisa rápida da tabela) têm prioridade. Os tooltips
   indicam o atalho e o guia F1 lista-os.
6. **Estado vazio com o passo seguinte** (`TableEmptyState`). Tabela sem dados: botão com a acção
   principal do cabeçalho (“Nova Cotação”), só se estiver visível e activa. Quando os filtros escondem
   todas as linhas: botão “Limpar filtros”, que esvazia a pesquisa e põe os combos na primeira opção.
   O cabeçalho só se liga à tabela quando o cartão tem uma tabela.
7. **Tema escuro.** Ao mudar de tema a janela principal é reconstruída, por isso os botões de contorno
   ficam com as cores do novo tema. O contraste fica garantido por teste.

8. **Combos dos filtros com o texto inteiro** (`SingleRowFilterLayout`). Muitos ecrãs fixavam os combos em
   120 a 145 px e “Todos os negócios” aparecia cortado. A barra de filtros dá a cada combo pelo menos a
   largura da sua opção mais comprida (calculada pelo próprio combo, até 260 px), mantendo a altura.
9. **Diálogos com a mesma hierarquia** (`ButtonStyles`). “Cancelar” é sempre o botão comum
   `ButtonStyles.cancelButton()`, com contorno e nunca vermelho. No `ModernFormDialog` (131 usos) só a
   confirmação tem cor cheia, e os botões extra ficam com contorno (texto vermelho se forem destrutivos,
   como “Rejeitar”).

A coluna estreita à esquerda das tabelas com linhas é a coluna de selecção (UA-06 a UA-08). Já era igual
em todas as tabelas e esconde-se quando a tabela está vazia.

## 3. Critérios de aceitação

| ID | Critério | Teste |
|---|---|---|
| TC-01 | Um só verde cheio; restantes com contorno; destrutivos com contorno e texto vermelho; nada some nem muda de ordem | `tc01_buttonHierarchy` |
| TC-02 | “Remover linha” dos editores continua à vista | `tc02_editorRemoveStaysVisible` |
| TC-03 | O mesmo estado tem a mesma cor nos dois renderizadores | `tc03_sharedStatusVocabulary` |
| TC-04 | Texto do rodapé igual e estável, também vazio | `tc04_pagerText` |
| TC-05 | Botão de espreitar no fim dos filtros, com contorno | `tc05_peekToggleAtEndOfFilters` |
| TC-06 | Ctrl+N, F5 e Ctrl+F em todos os cabeçalhos; ecrã escondido não responde; tooltips com o atalho | `TableShortcutsAndEmptyStateHarnessTest.tc06_sameShortcutsOnEveryHeader` |
| TC-07 | Tabela vazia oferece a acção principal; filtros que escondem tudo oferecem “Limpar filtros” | `tc07_emptyStateOffersTheNextStep` |
| TC-08 | Botões de contorno legíveis no tema escuro | `tc08_outlineReadableInDarkTheme` |
| TC-09 | Combo de filtro mostra a opção inteira mesmo com largura fixa estreita | `TabNamesAndCombosHarnessTest.tc09_comboShowsWholeOption` |
| TC-10 | “Cancelar” com contorno; extras de diálogo com contorno, vermelho só no texto | `DialogButtonsHarnessTest.tc10_dialogButtonHierarchy` |
| TC-11 | Nenhum diálogo cria o “Cancelar” à mão; o `ModernFormDialog` aplica a regra | `tc11_everyDialogUsesTheSharedCancel` |

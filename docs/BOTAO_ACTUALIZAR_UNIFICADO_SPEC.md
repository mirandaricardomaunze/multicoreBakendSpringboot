# Botão "Actualizar" unificado

Estado: implementado em 2026-11-05.
Harness: `desktop/src/test/java/mz/multicore/erp/gui/RefreshButtonUnificationHarnessTest.java`.

## Problema

No Inventário Físico, o botão Actualizar estava na linha de filtros e não no cabeçalho do card,
como em Pedidos. Uma pesquisa no resto do sistema encontrou 31 botões "Actualizar" construídos à mão
com `createSecondaryButton` / `createPrimaryButton`, em vez de `UIHelper.createRefreshButton(...)`:

- três no POS (histórico de vendas, importar cotação, histórico de sessões) usavam a cor primária;
- o ícone variava entre 12, 13 e 14 px;
- faltavam a dica "Carregar os dados mais recentes" e o nome acessível.

## Regras

| Id | Regra |
|----|-------|
| BAU-01 | Todo o botão com o texto "Actualizar" é criado por `UIHelper.createRefreshButton(acção)`. A única construção directa fica na própria fábrica (`UiFormComponentStyler`). |
| BAU-02 | Uma dica específica do ecrã pode substituir a padrão com `setToolTipText(...)` depois de criado o botão. |
| BAU-03 | Em cards com `UIHelper.tableCardTop`, o Actualizar fica entre as acções do cabeçalho, como primeira acção, e não na linha de filtros. |
| BAU-04 | Os filtros do Inventário Físico ficam numa só linha (`TableFilter.bar`). Sem espaço, a pesquisa e os selectores encolhem. |

Os botões com outro texto ("Actualizar Mapa", "Actualizar Progresso") e as confirmações de
formulários ("Actualizar" como gravar) ficam fora desta regra.

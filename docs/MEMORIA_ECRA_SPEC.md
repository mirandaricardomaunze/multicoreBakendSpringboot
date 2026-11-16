# Memória do ecrã: tabelas e janela principal

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/ScreenMemoryHarnessTest.java`

## Problema

O operador ajustava larguras e ordem das colunas, ordenava uma tabela, redimensionava a janela, e
tudo se perdia ao reabrir o ecrã ou o programa. Só 2 ecrãs guardavam alguma coisa.

## Regras

| ID    | Regra |
|-------|-------|
| ME-01 | Larguras, ordem das colunas e ordenação de cada tabela voltam quando a tabela reaparece. |
| ME-02 | Uma tabela é identificada pelos nomes das colunas (ordem do modelo): tabelas diferentes não partilham preferências. |
| ME-03 | "Repor colunas" (menu do cabeçalho, botão direito) volta à disposição do próprio ecrã e esquece o que estava guardado; os renderizadores das colunas mantêm-se. |
| ME-04 | Preferências corrompidas são ignoradas sem afectar o ecrã. |
| ME-05 | A janela principal volta ao tamanho, posição e estado (maximizada/normal) da última sessão; maximizada por omissão ou se a posição guardada já não cabe em nenhum ecrã. |
| ME-06 | Tudo é instalado centralmente (`UIHelper.styleTable` e `DesktopLauncher`); nenhum ecrã precisa de código próprio. |

## O que se grava, e quando

Só o que o operador faz: largar o rato no cabeçalho depois de redimensionar ou mover uma coluna, ou
mudar a ordenação. O redimensionamento automático que acompanha a janela não é gravado. A disposição
guardada é aplicada na primeira vez que a tabela aparece, depois de o ecrã ter posto as suas larguras
por omissão. Fica em `Preferences` do utilizador do Windows (`mz/multicore/erp/ui/tables` e
`.../ui/window`).

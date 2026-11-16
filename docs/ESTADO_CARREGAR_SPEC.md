# Estado "a carregar" nas tabelas

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/TableLoadingStateHarnessTest.java`

## Problema

Enquanto os dados vinham do servidor, as tabelas mostravam "Sem registos." — uma afirmação falsa.
Só o cursor do rato mudava para espera. Havia ainda textos soltos ("A carregar frotas...") feitos
à mão em alguns ecrãs.

## Regras

| ID    | Regra |
|-------|-------|
| EC-01 | Uma tabela vazia cujos dados estão a caminho mostra "A carregar…" com uma barra animada, sem botão de acção; quando a carga termina volta ao estado vazio normal. |
| EC-02 | Uma tabela que já tem linhas continua a mostrá-las durante a actualização (sem piscar, sem tapar). |
| EC-03 | Várias cargas em paralelo são contadas: a tabela só deixa de carregar quando a última termina. |
| EC-04 | As tabelas dentro de separadores do ecrã que carrega também ficam marcadas. |
| EC-05 | É o `UIHelper.loadAsync` central que marca e liberta as tabelas da sua área (`TableLoadingState.begin/end`). Nenhum ecrã precisa de código próprio. |

## Como funciona

`UIHelper.loadAsync(area, …)` procura as `JTable` dentro de `area` e aumenta um contador em cada uma.
O `TableEmptyState`, já instalado em todas as tabelas pelo `styleScrollPane`, escuta esse contador e
troca o ícone pela barra animada. No fim da carga, com sucesso ou erro, o contador desce.

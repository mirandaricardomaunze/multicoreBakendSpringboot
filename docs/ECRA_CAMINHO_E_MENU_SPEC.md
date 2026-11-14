# Caminho na topbar, cabeçalho de tabela numa fila e menu lateral recolhido

**Código:** SPEC-ECRA-CAMINHO  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  
**Data:** 2026-11-13  
**Estado:** APROVADO / IMPLEMENTADO  
**Harness:** `ScreenChromeHarnessTest` (componentes) e `MainFrameBreadcrumbHarnessTest`  
**Relacionado:** `docs/KPI_FAIXA_COMPACTA_SPEC.md`

## 1. Problema

Num portátil 1366×768, em Stock › Transferências, o nome “Transferências entre Armazéns” aparecia no
separador e outra vez como título do cartão da tabela, numa fila só para ele (≈ 42 px). Os filtros
ficavam noutra fila por baixo. O menu lateral abria sempre com 240 px, e as 9 colunas da tabela
cortavam texto.

## 2. Regras

1. **Caminho na topbar.** A topbar mostra `Módulo › Separador` (ex. `Stock › Transferências entre
   Armazéns`). O `MainFrameBreadcrumb` encontra o primeiro `JTabbedPane` visível do ecrã activo e segue
   a mudança de separador. Ecrãs sem separadores mostram só o módulo. Separador comprido termina em
   “…” (máx. 380 px) com o nome completo no tooltip.
2. **Cabeçalho comum em todos os ecrãs** (`UIHelper.tableCardTop`, regra única em `TableCardTop`;
   `ResponsiveSupport.cardTop` dos ecrãs de sector delega nele). Ordem fixa: título à esquerda, filtros,
   acções à direita com Actualizar primeiro. **Uma fila quando os filtros cabem à largura natural;**
   senão duas filas, título e acções em cima e filtros com a largura toda em baixo. Os filtros nunca são
   esmagados (“Tod…”). Título e botões alinham com a primeira linha de campos; se os filtros tiverem
   mais uma linha (por exemplo um aviso), essa fica por baixo sem os empurrar. Numa coluna estreita
   (lista e detalhe) onde nem título e botões cabem lado a lado, os botões passam a uma linha própria,
   alinhados à direita, e quebram em mais linhas se for preciso: nunca são cortados.
3. **Título repetido sai.** Esconde-se quando contém o nome do separador em palavras inteiras (“Recibos
   Emitidos (Liquidações)” em “Recibos”; “&” lê-se “e”, por isso “Central de Pedidos e Separação” em
   “Pedidos & Separação”), ou quando o nome do separador começa pelo título. Contadores “(3)” não contam.
   Se o separador tiver mais de um cabeçalho de tabela, os títulos ficam todos: são eles que distinguem
   as tabelas.
4. **Menu lateral.** Sem escolha guardada, abre recolhido (64 px, só ícones com tooltip) em ecrãs com
   menos de 1440 px de largura lógica, e aberto nos outros. O clique do utilizador no botão ou Ctrl+B
   fica guardado nas preferências de UI e passa a valer sempre. O recolher automático do POS não é
   gravado. Itens e secções acrescentados depois (módulos de sector) seguem o estado actual do menu.

## 3. Critérios de aceitação

| ID | Critério | Teste |
|---|---|---|
| ECR-01 | Título, filtros e acções numa só fila | `ScreenChromeHarnessTest.ecr01_oneRowHeader` |
| ECR-02 | Título escondido quando repete o separador; fica quando diz outra coisa ou quando há várias tabelas no separador | `ecr02_titleHiddenWhenTabRepeatsIt` |
| ECR-03 | Sem separador, o título continua visível | `ecr03_titleStaysWithoutTabs` |
| ECR-04 | Topbar mostra `Módulo › Separador`; outro módulo limpa o separador | `ecr04_topBarPath` |
| ECR-05 | Menu recolhido abaixo de 1440 px; a escolha guardada ganha sempre | `ecr05_sidebarStartState` |
| ECR-06 | Só o clique do utilizador é gravado; itens tardios seguem o estado | `ecr06_onlyUserTogglesAreRemembered` |
| ECR-07 | O caminho segue o separador do ecrã visível e some num ecrã sem separadores | `MainFrameBreadcrumbHarnessTest.ecr07_pathFollowsSelectedTab` |
| ECR-08 | Filtros nunca esmagados: uma fila quando cabem, segunda fila com a largura toda quando não; centrados com os botões | `ecr08_filtersNeverSqueezed` |
| ECR-10 | Com uma linha extra nos filtros, título e botões alinham com a primeira linha de campos | `ecr10_alignsWithFirstLineOfFilters` |
| ECR-11 | Auditoria de todos os ecrãs: cada módulo e ecrã de sector, todos os separadores, a 1270 px e 1100 px; nenhum cabeçalho com título ou botões fora da linha, filtros esmagados, peças sobrepostas ou botões cortados | `AllScreensHeaderAuditTest` (relatório em `desktop/target/all-screens-header-audit.txt`) |
| ECR-09 | Todo o ecrã com filtros (excepto diálogos) usa o cabeçalho comum | `ecr09_everyScreenUsesTheSharedHeader` |

## 4. Fora de âmbito

- Ecrãs com navegação própria sem `JTabbedPane` mostram só o módulo no caminho.
- Separadores dentro de separadores: o caminho mostra só o primeiro nível.

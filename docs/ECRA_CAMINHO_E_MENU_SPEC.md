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
2. **Cabeçalho da tabela numa fila.** `UIHelper.tableCardTop` (91 tabelas, regra única em
   `TableCardTop`) põe título, filtros e acções na mesma fila. Os filtros encolhem quando falta espaço
   (`SingleRowFilterLayout`).
3. **Título repetido sai.** O título do cartão esconde-se quando um separador que o contém tem o mesmo
   nome, ignorando contadores como “(3)”, ou quando o nome do separador começa pelo título
   (“Movimentos & Rastreabilidade” cobre “Movimentos”). Um título que acrescenta informação
   (“Clientes Bloqueados” num separador “Clientes”) fica.
4. **Menu lateral.** Sem escolha guardada, abre recolhido (64 px, só ícones com tooltip) em ecrãs com
   menos de 1440 px de largura lógica, e aberto nos outros. O clique do utilizador no botão ou Ctrl+B
   fica guardado nas preferências de UI e passa a valer sempre. O recolher automático do POS não é
   gravado. Itens e secções acrescentados depois (módulos de sector) seguem o estado actual do menu.

## 3. Critérios de aceitação

| ID | Critério | Teste |
|---|---|---|
| ECR-01 | Título, filtros e acções numa só fila | `ScreenChromeHarnessTest.ecr01_oneRowHeader` |
| ECR-02 | Título escondido quando o separador o repete; fica quando acrescenta informação | `ecr02_titleHiddenWhenTabRepeatsIt` |
| ECR-03 | Sem separador, o título continua visível | `ecr03_titleStaysWithoutTabs` |
| ECR-04 | Topbar mostra `Módulo › Separador`; outro módulo limpa o separador | `ecr04_topBarPath` |
| ECR-05 | Menu recolhido abaixo de 1440 px; a escolha guardada ganha sempre | `ecr05_sidebarStartState` |
| ECR-06 | Só o clique do utilizador é gravado; itens tardios seguem o estado | `ecr06_onlyUserTogglesAreRemembered` |
| ECR-07 | O caminho segue o separador do ecrã visível e some num ecrã sem separadores | `MainFrameBreadcrumbHarnessTest.ecr07_pathFollowsSelectedTab` |

## 4. Fora de âmbito

- Ecrãs com navegação própria sem `JTabbedPane` mostram só o módulo no caminho.
- Separadores dentro de separadores: o caminho mostra só o primeiro nível.

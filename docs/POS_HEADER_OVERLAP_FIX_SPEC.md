# Especificação — Resolução de Sobreposição no Cabeçalho do POS

> **Status:** Aprovado e Canónico  
> **Data:** 2026-10-04  
> **Contexto:** Resolução definitiva da colisão/sobreposição física de botões observada no topo do `POSPanel` ao abrir sessão de caixa em ecrãs com largura padrão (1366x768 / 1280x720) sob tema claro.

---

## 1. Diagnóstico do Problema Visual

Na captura de ecrã submetida pelo utilizador (`POS — Caixa`), observa-se que os botões `[Cotação F7]` e `[Fidelidade]` colidem e sobrepõem-se directamente sobre o botão `[Passar Turno]`, ocultando ainda os botões `[Fechos (Z)]` e o botão de actualização `[↻]`.

### Causas-Raiz Identificadas:
1. **Proliferação Excessiva de Botões Horizontais numa Linha Única:**
   - À esquerda (`segmented`): `Venda POS` (~120px) + `Histórico de Vendas` (~165px) + `Cotação` (~125px) + `Fidelidade` (~120px) = **~530px**.
   - À direita (`sessionActions`): `↻` (36px) + `Fechos (Z)` (~120px) + `Passar Turno` (~135px) + `Sangria / Suprimento` (~185px) + `Fechar Caixa` (~145px) = **~621px**.
   - **Largura total necessária:** ~530px + ~621px = **1151px**.
   - Em monitores comuns de 1366px ou 1280px, com a barra lateral esquerda aberta (~240px) e margens do painel (~36px), a largura disponível para a barra do topo é de apenas **~1000px a 1090px**.
2. **Comportamento Falho do `BorderLayout` na Sobreposição:**
   - O `topBar` utilizava `BorderLayout(12, 0)` (`segmented` em `WEST`, `sessionActions` em `EAST`).
   - Quando `largura_disponivel < WEST + EAST`, o AWT `BorderLayout` atribui `WEST.bounds = (0, 0, westWidth, h)` e `EAST.bounds = (totalWidth - eastWidth, 0, eastWidth, h)`.
   - Como os dois retângulos se intersectam fisicamente, o Swing pinta um painel sobre o outro, gerando a sobreposição caótica de botões vista pelo utilizador.
3. **Incoerência Semântica:**
   - `Cotação` e `Fidelidade` foram inseridos dentro de `segmented` (que é o seletor de vistas da janela), criando uma faixa com 7 cores diferentes em choque (Azul, Roxo, Ciano, Violeta, Verde, Âmbar e Vermelho), violando a diretiva expressa do utilizador: *"SIM BASTA NAO FAZER RUIDO NA UI , QUERO QUE SEJA PROFISSIONAL"*.

---

## 2. Decisões Arquiteturais e de Design

1. **Adesão Estrita ao Padrão de Cabeçalho do ERP:**
   - Conforme regra geral do [AGENTS.md](../AGENTS.md): *"no cabeçalho, evitar filas longas de botões que colidem com títulos (máx. 2-3 botões abertos, agrupando acções de linha seleccionada em ActionMenuButton com limite estrito de no máximo 5 opções por menu)"*.
2. **Reorganização de Vistas e Ações:**
   - **Seletor de Vistas (`segmented` à esquerda):**
     - `Venda POS` (Aba ativa)
     - `Histórico de Vendas` (Aba de histórico)
     - `Operações` ([ActionMenuButton](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/ActionMenuButton.java)) agrupando:
       - `Importar Cotação (F7)` (ícone `fas-file-import`)
       - `Programa de Fidelidade` (ícone `fas-star`)
       - `Histórico de Fechos (Z)` (ícone `fas-file-invoice-dollar`)
     - `Contingência` (apenas quando houver registos pendentes offline)
   - **Ações de Sessão (`sessionActions` à direita):**
     - Botão de actualização `[↻]`
     - Quando caixa fechada: `[Abrir Caixa]` (Verde canónico)
     - Quando caixa aberta: `[Passar Turno]` (Verde neutro) + `[Sangria / Suprimento]` (Âmbar) + `[Fechar Caixa [Z]]` (Vermelho perigo)
3. **Substituição de `BorderLayout` por `GridBagLayout` Anti-Colisão:**
   - `topBar` passa a usar `GridBagLayout`:
     - Coluna 0 (`WEST`): `segmented` (`weightx = 0.0`, `anchor = WEST`)
     - Coluna 1 (`CENTER`): `Box.createHorizontalGlue()` (`weightx = 1.0`, `fill = HORIZONTAL`)
     - Coluna 2 (`EAST`): `sessionActions` (`weightx = 0.0`, `anchor = EAST`)
   - Em `GridBagLayout`, as colunas mantêm delimitação física rígida: componentes em colunas distintas **nunca** partilham as mesmas coordenadas X nem se sobrepõem visualmente, colapsando graciosamente em telas compactas.
4. **Largura Calibrada e Ganho de Espaço:**
   - Lado esquerdo: ~400px.
   - Lado direito (com sessão aberta): ~500px.
   - Total ocupado: **~900px**.
   - Em tela de 1090px, sobram **~190px de respiro visual limpo**, sem qualquer choque visual nem ruído.

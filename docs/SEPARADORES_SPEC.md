# Separadores: "Mais ▾", aspecto sóbrio e memória

**Código:** SPEC-SEPARADORES  
**Módulo:** `desktop` (`mz.multicore.erp.gui.components`)  
**Data:** 2026-11-14  
**Estado:** APROVADO / IMPLEMENTADO  
**Harness:** `desktop/src/test/java/mz/multicore/erp/gui/components/TabStripHarnessTest.java`  
**Relacionado:** `docs/ECRA_CAMINHO_E_MENU_SPEC.md`, `TabStripFitsTest`

## 1. Problema

Num ecrã de 1366 px com o menu recolhido cabem 1270 px de separadores. Medido em 2026-11-14:

| Ecrã | Largura | Cabe? |
|---|---|---|
| Configurações (9) | 1655 px | não, ~3 escondidos |
| Compras (7) | 1281 px | não, o último cortado |
| Vendas (10), Stock (7), RH (11) | 1157–1211 px | só com o menu recolhido |

O que não cabia ficava atrás de setas: um separador escondido não avisa, só desaparece. Cada separador
tinha o ícone numa cor diferente e todo o texto em negrito. Ao voltar a um módulo abria sempre no
primeiro separador.

## 2. Regras (todas em `UIHelper.styleTabbedPaneMulticore`, usado por todos os ecrãs)

1. **"Mais ▾" em vez de setas** (`UiTabOverflow`). Quando a barra não cabe, aparece "Mais ▾" no fim.
   A lista mostra os separadores que não estão totalmente visíveis. Escolher um selecciona-o, e a barra
   desliza até ele. O controlo nunca fica desactivado. A roda do rato sobre a barra continua a deslizar.
2. **Aspecto sóbrio** (`UiTabIcons`). Os ícones são desenhados em `TEXT_MUTED` e só o separador activo
   usa `ACCENT`. Só o activo fica a negrito. As larguras continuam medidas a negrito, para os
   separadores não saltarem ao mudar de selecção.
3. **Memória do separador** (`UiTabMemory`). Cada barra da janela principal reabre no último separador
   usado, guardado nas preferências de UI e identificado pelos nomes dos separadores (sem contadores).
   Não se aplica a diálogos. Um separador escolhido pelo programa antes de o ecrã aparecer (por exemplo
   o atalho para Transferências) tem prioridade. Um separador desactivado ou que já não existe é ignorado.

## 3. Critérios de aceitação

| ID | Critério | Teste |
|---|---|---|
| TAB-01 | Barra que não cabe mostra "Mais ▾" (sem setas); a lista tem os escondidos e selecciona-os | `tab01_overflowListsHiddenTabs` |
| TAB-02 | Quando tudo cabe, não há "Mais" | `tab02_noOverflowWhenEverythingFits` |
| TAB-03 | "Mais" nunca fica desactivado | `tab03_moreStaysEnabled` |
| TAB-04 | Ícones na cor sóbria, destaque no activo, original intacto | `tab04_quietIcons` |
| TAB-05 | Memória pela lista de nomes, ignora contadores e separadores desactivados | `tab05_memoryKeyAndLookup` |
| TAB-06 | As três regras estão ligadas no estilo único; diálogos e escolha do programa respeitados | `tab06_wiredInTheSingleStyle` |

## 4. Próximo passo possível

Nomes curtos (até cerca de 20 caracteres), com o nome completo no tooltip e no caminho da topbar. Exige
mexer no texto de cada ecrã e ajustar a regra do título repetido de `ECRA_CAMINHO_E_MENU_SPEC.md`.

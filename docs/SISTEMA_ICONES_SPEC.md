# SPEC — Padronização e Modernização do Sistema de Ícones

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`)  
**Backend:** contratos e regras de negócio permanecem inalterados.

---

## 1. Objectivo

Dotar o Multicore ERP de um subsistema de ícones vetoriais robusto, padronizado, seguro contra falhas em tempo de execução e acessível, garantindo coerência visual e cromática em todos os ecrãs, formulários, tabelas e barras de ferramentas, em perfeita harmonia com os temas Claro e Escuro.

---

## 2. Princípios e Regras Fundamentais

1. **Resiliência e Fallback Gracioso (Zero Crash Policy)**:
   - O carregamento de ícones nunca deve interromper a inicialização de um painel, diálogo ou aplicação por motivo de código inválido, nulo ou erro tipográfico.
   - Qualquer falha na resolução do código devolve um ícone canónico de fallback (`fas-question-circle` ou `fas-circle`) e regista aviso nos logs.

2. **Escala Canónica de Tamanhos (Size Tokens)**:
   - Proibição de tamanhos literais arbitrários no código. Toda a interface deve utilizar a escala semântica:
     - `UIHelper.ICON_XS = 12` (Badges, micro tags, status inline).
     - `UIHelper.ICON_SM = 14` (Ações de tabela, botões compactos, barras secundárias).
     - `UIHelper.ICON_MD = 16` (Botões de topo/formulário, tabs, menus de contexto).
     - `UIHelper.ICON_LG = 20` (Sidebar, barra de contexto, toggles principais).
     - `UIHelper.ICON_XL = 24` (Títulos de diálogos, cabeçalhos de secção, cartões de KPI).
     - `UIHelper.ICON_HERO = 48` (Empty states, ecrãs de sucesso/erro e ilustrações).

3. **Adaptação Dinâmica ao Tema (Light & Dark)**:
   - A sobrecarga canónica `UIHelper.icon(code, size)` assume a cor primária de texto do tema ativo (`UIHelper.TEXT_LIGHT`), garantindo que ícones em botões/fundos claros no Modo Claro não fiquem invisíveis.
   - Sobrecargas com cor explícita respeitam a cor fornecida.

4. **Semântica Cromática das Ações**:
   - 🟢 **Verde (`APPROVED_GREEN` / `ACCENT_GREEN`):** Novo registo (`fas-plus`), Guardar, Concluir/Pagar, Aprovar.
   - 🔵 **Azul (`ACCENT_BLUE` / `INFO_BLUE`):** Editar (`fas-pen`), Imprimir (`fas-print`), Detalhes (`fas-eye`), Sincronizar (`fas-sync-alt`).
   - 🔴 **Vermelho (`REJECTED_RED` / `ERROR_RED`):** Eliminar (`fas-trash`), Cancelar/Anular, Suspender, Bloquear, Alertas críticos.
   - 🟠 **Laranja / Âmbar (`PENDING_YELLOW` / `WARNING_ORANGE`):** Avisos, Pendências, Stock baixo, Rascunhos.
   - ⚪ **Neutro / Muted (`TEXT_MUTED` / `TEXT_LIGHT`):** Fechar, Ações secundárias, Navegação geral.

5. **Ícones Compostos com Badge (`BadgedIcon`)**:
   - Suporte centralizado a ícones com sobreposição de contadores numéricos ou pontos indicadores de estado em tempo real, com acabamento anti-aliasing e legibilidade.

6. **Acessibilidade para Botões Icon-Only**:
   - Qualquer botão que exiba exclusivamente um ícone deve possuir obrigatoriamente `toolTipText` e descrição textual acessível no `AccessibleContext`.

---

## 3. Especificação da API (`UIHelper` & `BadgedIcon`)

```java
// Tokens de dimensão
public static final int ICON_XS = 12;
public static final int ICON_SM = 14;
public static final int ICON_MD = 16;
public static final int ICON_LG = 20;
public static final int ICON_XL = 24;
public static final int ICON_HERO = 48;

// Métodos de criação e composição
public static Icon icon(String code, int size, Color color);
public static Icon icon(String code, int size);
public static Icon badgedIcon(String code, int size, Color color, int badgeCount, Color badgeBg);
public static Image iconImage(String code, int size, Color color);
public static JButton createIconButton(String code, int size, String tooltip, Runnable action);
```

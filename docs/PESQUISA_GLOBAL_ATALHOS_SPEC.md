# Especificação Técnica — Pesquisa Global Rápida (`Ctrl+K`), Atalhos (`F1`/`F11`) e Produtividade

Este documento define a especificação arquitetural e os requisitos de interface para o sistema de pesquisa global estilo Spotlight / Command Palette (`GlobalSearchDialog`), guia de atalhos (`ShortcutHelpDialog`) e modos de produtividade de ecrã completo no Multicore ERP.

---

## 1. Objectivos e Filosofia

1. **Velocidade Operacional (Zero-Friction):** Permitir que qualquer operador ou gestor navegue para qualquer módulo, consulte dados rápidos ou execute uma ação frequente em menos de 2 segundos, utilizando apenas o teclado.
2. **Descoberta Intuitiva:** Exibir atalhos contextuais e comandos de forma visualmente rica e categorizada.
3. **Consistência Visual:** Total aderência ao Design System do Multicore ERP (`UIHelper`, `Theme`, `ModernPanel`, `ModernButton`, paleta de cores canónica e acessibilidade WCAG AA).

---

## 2. Paleta de Comandos e Pesquisa Global (`GlobalSearchDialog`)

### 2.1 Modelo de Dados (`SearchItem`)
Cada item indexável na pesquisa global é representado por:
- `id`: Identificador único (ex.: `nav_pos`, `nav_stock`, `action_new_sale`).
- `title`: Nome visível do módulo ou ação (ex.: "POS — Caixa", "Vendas & Faturação").
- `category`: Categoria de agrupamento ("Módulos", "Ações Rápidas", "Configurações").
- `shortcutHint`: Dica de atalho associada, se aplicável (ex.: "Ctrl+B", "F2", "F9").
- `icon`: Ícone canónico `FontIcon`.
- `accentColor`: Cor semântica do módulo/ação.
- `action`: `Runnable` a executar no EDT quando selecionado.
- `keywords`: Lista de termos sinónimos para facilitar a localização (ex.: "vendas", "caixa", "fatura", "recibo", "produtos", "armazém").

### 2.2 Algoritmo de Filtragem
1. Quando a consulta de pesquisa (`query`) estiver vazia ou com espaços em branco, todos os itens ou os itens principais/recentes são exibidos por ordem de relevância.
2. A pesquisa efetua correspondência insensível a maiúsculas/minúsculas (`toLowerCase()`) no título, categoria e lista de palavras-chave.
3. O primeiro resultado da lista é selecionado automaticamente após cada digitação.

### 2.3 Interação e Teclado
- `Up` / `Down`: Move o cursor de seleção na lista de resultados sem perder o foco no campo de texto.
- `Enter`: Executa o item atualmente selecionado e fecha o diálogo imediatamente.
- `Escape`: Fecha o diálogo sem executar nenhuma ação.
- Clique com o rato: Seleciona e executa o item clicado.

---

## 3. Guia Visual de Atalhos (`ShortcutHelpDialog`)

O modal `ShortcutHelpDialog` exibe em grelha de cartões os atalhos disponíveis no sistema organizados em grupos temáticos:

1. **Gerais & Navegação:**
   - `Ctrl+K`: Pesquisa Global / Paleta de Comandos
   - `Ctrl+B`: Alternar Menu Lateral (Expandido / Recolhido)
   - `F1`: Guia de Atalhos
   - `F11`: Alternar Modo Ecrã Completo (Fullscreen / Quiosque)
   - `Esc`: Fechar Janelas, Modais ou Menus
2. **Ponto de Venda (POS / Balcão):**
   - `F2`: Pesquisar Produto no Catálogo
   - `F3`: Foco no Leitor de Código de Barras
   - `F4`: Selecionar / Trocar Cliente
   - `F6`: Alterar Quantidade da Linha
   - `F9`: Finalizar Venda e Abrir Pagamento
   - `Delete`: Remover Produto do Carrinho

---

## 4. Integração no `TopNavBar` e `MainFrame`

- **Barra de Topo (`TopNavBar`):** Inclui um botão/pílula de pesquisa central com o texto *"Pesquisar módulos e ações... (Ctrl+K)"*, permitindo acionamento tanto por rato como por atalho de teclado.
- **Janela Principal (`MainFrame`):** Registra os atalhos globais no `RootPane` com `JComponent.WHEN_IN_FOCUSED_WINDOW`:
  - `KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())` -> Abre `GlobalSearchDialog`.
  - `KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)` -> Abre `ShortcutHelpDialog`.
  - `KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)` -> Alterna `toggleFullScreen()`.

# SPEC — Lote de UX das tabelas

**Criado em:** 2026-07-23
**Camada:** UI Swing (`com.phcpro.gui.components`)
**Sem backend, sem migração.**

Quatro melhoras transversais às tabelas, ligadas **centralmente** em
`UIHelper.styleScrollPane(JScrollPane)` (quando o conteúdo é `JTable`), no espírito DRY já usado
por `TableNavigator` e `maybeAddListingFooter`.

---

## 1. Barra de navegação — auto-esconder + teclado

- A barra lateral (`TableNavigator`) passa a **aparecer só quando a tabela transborda** (há mais
  linhas do que cabem — `maximum - minimum > extent` na `JScrollBar` vertical). Tabela curta → sem
  barra, sem ruído. Um `ChangeListener` no modelo da scrollbar alterna a visibilidade.
- **Atalhos de teclado** na tabela (listagens são só-leitura, logo é seguro sobrepor):
  `Home`→topo, `End`→fundo, `PageUp`/`PageDown`→página. Mapeados para as mesmas acções da barra.

## 2. Estados vazios (`TableEmptyState`)

- Tabela com **0 linhas** mostra uma mensagem centrada em vez de grelha em branco.
- Texto por omissão **"Sem registos."**; cada tabela pode personalizar via
  `table.putClientProperty("emptyText", "Sem encomendas.")` (sem tocar no componente).
- Implementado como overlay centrado sobre o viewport (só visível quando vazio — não tapa dados).
- Reage a alterações do modelo (`TableModelListener`) e à troca de modelo.

## 3. Menu de contexto (`TableContextMenu`)

- **Botão direito** numa linha selecciona-a e abre um menu **genérico** (sem depender do domínio):
  **Copiar linha**, **Copiar célula**, **Ir para o topo**, **Ir para o fundo**.
- Acções específicas do domínio (Ver detalhes / Imprimir / Anular) continuam nos botões de cada
  painel — o menu genérico é o gesto "como noutros sistemas" sem cablagem painel-a-painel.

## 4. Feedback de carregamento (`UIHelper.loadAsync`)

- Helper reutilizável: busca os dados **fora do EDT** (`SwingWorker`), com **cursor de espera** na
  janela, e aplica o resultado no EDT quando chega. A UI deixa de parecer congelada nas chamadas
  HTTP do cliente-fino.
- Assinatura: `loadAsync(JComponent scope, Callable<T> fetch, Consumer<T> onDone)`.
- **Adopção**: aplicado à aba **Guias de Remessa** como referência; os restantes `loadXxxTable`
  adoptam o mesmo padrão incrementalmente (documentado, não forçado num só passo).

---

## Camadas / ficheiros

| Ficheiro | Papel |
|----------|-------|
| `TableNavigator.java` | auto-hide + atalhos de teclado |
| `TableEmptyState.java` (novo) | overlay de estado vazio |
| `TableContextMenu.java` (novo) | menu de botão direito |
| `UIHelper.java` | `styleScrollPane` (hooks) + `loadAsync` |

## Estética (CONVENTIONS)

- Ícones vectoriais `UIHelper.icon("fas-…")`, nunca emojis.
- Cores por tema (`BG_CARD`, `BORDER`, `TEXT_MUTED`, `ACCENT`).

## Fora de âmbito

- Adopção de `loadAsync` em todos os painéis (incremental).
- Acções de domínio no menu de contexto (ficam nos botões).

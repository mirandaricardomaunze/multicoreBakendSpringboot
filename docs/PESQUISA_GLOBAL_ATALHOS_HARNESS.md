# Harness de Testes — Pesquisa Global Rápida e Atalhos

Este documento formaliza os testes automatizados necessários para validar a implementação da pesquisa global (`GlobalSearchDialog`), guia de atalhos (`ShortcutHelpDialog`), atalhos de teclado e integração na interface do Multicore ERP.

---

## Critérios de Conformidade e Casos de Teste

| ID | Requisito | Critério de Aceitação | Teste Automatizado |
|---|---|---|---|
| **PGA-01** | Indexação de Comandos | `GlobalSearchDialog` indexa itens com título, categoria, atalho, ícone, ação e palavras-chave. | `GlobalSearchShortcutsHarnessTest.pga01_registersAndRetrievesIndexedItems` |
| **PGA-02** | Filtragem em Tempo Real | A pesquisa filtra corretamente itens por título, categoria e sinónimos/palavras-chave (case-insensitive). | `GlobalSearchShortcutsHarnessTest.pga02_filtersItemsByTitleCategoryAndKeywords` |
| **PGA-03** | Execução e Fechamento com Teclado | Pressionar `Enter` executa a ação do item selecionado e fecha o modal; pressionar `Esc` fecha sem executar. | `GlobalSearchShortcutsHarnessTest.pga03_executesSelectedItemOnEnterAndClosesOnEscape` |
| **PGA-04** | Guia de Atalhos (`ShortcutHelpDialog`) | O diálogo de atalhos estrutura corretamente os cartões de atalhos de Navegação/Sistema e POS. | `GlobalSearchShortcutsHarnessTest.pga04_shortcutHelpDialogPresentsCategorizedShortcuts` |
| **PGA-05** | Mapeamento de Teclas no `MainFrame` | O `MainFrame` registra `Ctrl+K`, `F1`, `F11` e `Ctrl+B` no seu `RootPane`. | `GlobalSearchShortcutsHarnessTest.pga05_mainFrameRegistersGlobalKeyBindings` |

---

## Execução

```bash
mvn test -pl desktop "-Dtest=GlobalSearchShortcutsHarnessTest"
```

# SPEC-NOTIFICATIONS-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Notificações

## 1. Contexto & Diagnóstico
O painel `NotificationsPanel.java` (239 linhas) é a página central de notificações e alertas operacionais da empresa ativa no ERP Multicore, sincronizada com o feed do sino via `NotificationReadStore`.

### Problemas Identificados
1. **Acoplamento de Funções de Apresentação e Texto:**
   - Métodos utilitários de resumo textual e conversão de linhas de tabela misturam-se com o ciclo de vida assíncrono e eventos de UI.
2. **Import Residual de JOptionPane:**
   - O ficheiro importa `javax.swing.JOptionPane` desnecessariamente (sem uso).

## 2. Objectivos da Refatoração
1. **Redução de Linhas do Orquestrador:** Reduzir `NotificationsPanel.java` de 239 linhas para $\le 150$ linhas (-37%).
2. **Extração de Coordenador de Apresentação:**
   - `NotificationsViewCoordinator.java` ($\le 110$ linhas): Coordenador responsável por mapeamento de linhas com leitura (`rowFor`), textos de sumário (`summaryText`), Quick Peek com atalho "Abrir Módulo" e gestão de marcação de lidas/não-lidas.
3. **Erradicação Absoluta de `JOptionPane`:** Remoção do import obsoleto e garantia de 0 chamadas.
4. **Preservação Integral de Layout e Contratos:**
   - Construtor público `NotificationsPanel(NotificationFeed, NotificationReadStore, Consumer<String>, IntConsumer)`.
   - Métodos estáticos puros preservados retrocompatíveis: `NotificationsPanel.rowFor(item, read)` e `NotificationsPanel.summaryText(total, unread)`.
   - `UIHelper.tableCardTop("Notificações", filterStack, refreshButton, readingMenu, openButton)`.
   - `UIHelper.ensureHeadersFit(table)`.
   - `TableQuickPeekController.install(table`.

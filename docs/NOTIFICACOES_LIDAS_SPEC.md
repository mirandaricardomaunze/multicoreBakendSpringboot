# SPEC — Notificações: marcar como lida / todas lidas (no sino)

**Criado em:** 2026-07-24
**Camada:** UI Swing (`mz.multicore.erp.gui`)
**Sem backend, sem migração.**

> Estende a feature de **notificações** (sino + página) — `NotificationFeed`/`NotificationsPanel`,
> em desenvolvimento paralelo. Aqui adiciona-se **marcar como lida** (por item) e **marcar todas
> como lidas** no **sino**.

## 1. Porquê estado do lado do cliente

As notificações são **derivadas** (agregam aprovações pendentes, stock baixo, validades e
assinatura em tempo real via `NotificationFeed`), **não** entidades persistidas com id. Logo não há
"read flag" no servidor. O estado lida/não-lida vive no **cliente**:

- **Chave estável** por notificação: `type|title|detail|when` (`NotificationReadStore.keyOf`).
- As chaves das lidas são guardadas nas **Preferences do utilizador** (sobrevivem ao reinício).
  À prova de falha: sem Preferences, funciona em memória.

## 2. Comportamento no sino (`MainFrame`)

- O **badge** passa a contar **não-lidas** (`unreadCount`), não o total.
- O popup do sino lista as **não-lidas** (até 5). Cada uma é um submenu com:
  **Abrir módulo** (navega) e **Marcar como lida** (`markRead` → badge decrementa).
- Entrada **Marcar todas como lidas** (`markAllRead` sobre todos os itens → badge = 0), desativada
  quando não há não-lidas.
- **Ver todas** continua a abrir a página de notificações.

## 3. Comportamento na página (`NotificationsPanel`)

A página partilha a **mesma instância** de `NotificationReadStore` que o sino (injectada pelo
`MainFrame`), pelo que os dois lados nunca divergem.

- Coluna **"Leitura"** por linha: `Por ler` / `Lida` (constantes `UNREAD_LABEL`/`READ_LABEL`).
  Nome deliberadamente diferente de "Estado" para não colidir com a coloração semântica de linha do
  `UIHelper.styleTable`.
- Dropdown **Leitura** (`Lidas e por ler` · `Por ler` · `Lida`) — filtro de coluna do `TableFilter`.
- Botões **Marcar como lida** (linha seleccionada) e **Marcar todas como lidas** (desactivado quando
  não há por ler). Ambos repintam a tabela **sem ir à rede** (`renderRows`).
- O resumo do cabeçalho passa a "N por ler de M notificações".
- **Badge sincronizado:** o construtor recebe um `IntConsumer` que o `MainFrame` liga ao badge do
  sino — marcar na página actualiza o sino sem novo pedido HTTP. No sentido inverso, marcar no sino
  reflecte-se na página no `onPanelSelected()` seguinte (que recarrega).

## 4. Limites conhecidos (v1)

- Duas notificações derivadas com exactamente os mesmos campos partilham chave (colisão rara; não há
  id na origem).
- Uma notificação lida que **volte a ser gerada com os mesmos campos** continua lida (é a mesma
  notificação, por definição da chave). Se o alerta mudar de texto/data, volta a aparecer por ler.

## 5. Ficheiros

| Ficheiro | Papel |
|----------|-------|
| `NotificationReadStore.java` (novo) | chave estável + conjunto de lidas (Preferences) |
| `MainFrame.java` | popup do sino (marcar como lida / todas) + badge de não-lidas |
| `NotificationsPanel.java` | coluna/filtro de leitura + marcar (uma/todas) + resumo de não-lidas |

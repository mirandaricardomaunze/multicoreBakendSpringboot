# HARNESS — Notificações: marcar como lida / todas lidas

Complementa [NOTIFICACOES_LIDAS_SPEC.md](NOTIFICACOES_LIDAS_SPEC.md). `NL-01..` automáticos
(`NotificationReadStoreTest` + `NotificationsPanelTest`, JUnit puro); `NL-50..` manuais (UI).

## Automáticos — `NotificationReadStoreTest` (modo memória)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| NL-01 | `keyOf` de um item | `"type|title|detail|when"` |
| NL-02 | `markRead(a)` | `isRead(a)` true; `isRead(b)` false |
| NL-03 | `unread` / `unreadCount` com um lido | devolve/contabiliza só os não-lidos |
| NL-04 | `markAllRead` | `unreadCount` = 0 |
| NL-05 | item novo com os mesmos campos de um já lido | considerado lido (chave estável) |

## Automáticos — `NotificationsPanelTest` (lógica pura da página)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| NL-06 | `rowFor(item, read)` | 5 colunas; a última é `Por ler` / `Lida` |
| NL-07 | `summaryText(total, unread)` | vazio · "N por ler de M…" · "todas lidas" (singular/plural) |

## Manuais (UI)

| ID    | Cenário | Evidência |
|-------|---------|-----------|
| NL-50 | Abrir o sino | Lista só as não-lidas; badge = nº de não-lidas |
| NL-51 | Submenu de uma notificação → **Marcar como lida** | Desaparece das não-lidas; badge decrementa |
| NL-52 | **Marcar todas como lidas** | Badge → 0; sino mostra "Não há notificações por ler." |
| NL-53 | Reiniciar a app | As lidas continuam lidas (Preferences) |
| NL-54 | Submenu → **Abrir módulo** | Navega para o módulo respetivo (comportamento anterior mantido) |
| NL-55 | Página de notificações | Coluna **Leitura** com `Por ler`/`Lida`; resumo "N por ler de M" |
| NL-56 | Página → seleccionar linha → **Marcar como lida** | Linha passa a `Lida`; **badge do sino decrementa na hora** |
| NL-57 | Página → dropdown **Leitura** = `Por ler` | Só as não-lidas na tabela |
| NL-58 | Página → **Marcar todas como lidas** | Todas `Lida`, botão desactiva, badge → 0 |
| NL-59 | Marcar no sino → abrir a página | A página mostra essa notificação como `Lida` |

## Definition of done

- `mvn -o clean compile` passa.
- `mvn -o test` passa (NL-01..07).
- Sem emojis (só `UIHelper.icon`).

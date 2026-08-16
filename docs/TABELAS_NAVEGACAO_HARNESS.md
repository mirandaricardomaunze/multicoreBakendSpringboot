# HARNESS — Barra lateral de navegação nas tabelas

Complementa [TABELAS_NAVEGACAO_SPEC.md](TABELAS_NAVEGACAO_SPEC.md). `TN-01..` automáticos
(`TableNavigatorTest`, JUnit puro sobre os helpers de scroll — molde `TableFilterTest`);
`TN-50..` manuais (UI).

## Automáticos — `TableNavigatorTest`

Usam uma `JScrollBar` vertical com modelo conhecido (`value`, `extent`, `min`, `max`), sem display.

| ID    | Cenário                                                        | Esperado |
|-------|----------------------------------------------------------------|----------|
| TN-01 | `top(bar)`                                                     | `value == minimum` |
| TN-02 | `bottom(bar)`                                                  | `value == maximum - extent` (clamp da `BoundedRangeModel`) |
| TN-03 | `pageDown(bar)` a partir do meio                               | `value` aumenta ~`visibleAmount` |
| TN-04 | `pageUp(bar)` a partir do meio                                 | `value` diminui ~`visibleAmount` |
| TN-05 | `pageUp(bar)` já no topo                                       | `value` fica em `minimum` (clamp, sem exceção) |
| TN-06 | `pageDown(bar)` já no fundo                                    | `value` fica em `maximum - extent` (clamp) |
| TN-07 | helpers com `null`                                             | não lançam (no-op) |
| TN-08 | detectar início/fim da área rolável                            | estado correcto para activar/desactivar botões |

## Manuais (UI)

| ID    | Cenário                                                        | Evidência |
|-------|----------------------------------------------------------------|-----------|
| TN-50 | Abrir uma aba com tabela longa (ex.: Faturas, Stock)          | Barra lateral com 4 botões à direita da tabela |
| TN-51 | Clicar **Topo** / **Fundo**                                    | Salta para o início / fim da lista |
| TN-52 | Clicar **Página acima** / **Página abaixo**                    | Rola uma página de cada vez |
| TN-53 | Tabela curta (cabe toda no ecrã)                               | Botões não rebentam nada; scroll fica no sítio |
| TN-54 | Confirmar em várias abas (Comercial, Stock, RH, Compras…)      | A barra aparece transversalmente (via `styleScrollPane`) |
| TN-55 | Chegar ao início/fim                                           | botões sem acção possível ficam desactivados |

## Definition of done

- `mvn -o clean compile` passa.
- `mvn -o test` passa (inclui `TableNavigatorTest`).
- Nenhum emoji em botões (só `UIHelper.icon`).
- `tasks/current.md` actualizado.

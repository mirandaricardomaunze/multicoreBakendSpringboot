# Harness — Actualização multiutilizador no fluxo comercial

| ID | Verificação | Resultado esperado |
|---|---|---|
| MU-01 | Abrir POS em dois terminais e alterar caixa/dados num deles | `Actualizar` reflecte o estado no outro terminal. |
| MU-02 | Criar uma fatura noutro utilizador | Facturação apresenta-a após `Actualizar`. |
| MU-03 | Criar ou mudar um pedido noutro utilizador | Pedidos apresenta o estado mais recente após `Actualizar`. |
| MU-04 | Aprovar/rejeitar uma guia noutro utilizador | Guias apresenta o estado mais recente após `Actualizar`. |
| MU-05 | Emitir/aprovar uma nota noutro utilizador | A listagem correspondente actualiza sem reabrir a área. |
| MU-06 | Concluir qualquer operação local | As listagens afectadas recarregam automaticamente. |
| MU-07 | Simular API lenta | A interface permanece responsiva durante a recarga. |


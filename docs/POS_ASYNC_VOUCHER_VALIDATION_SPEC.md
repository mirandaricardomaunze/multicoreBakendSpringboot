# SPEC — Validação Assíncrona de Vales no POS

## Problema

O diálogo de pagamento consultava o vale por HTTP directamente em listeners do Swing. Uma ligação
lenta podia bloquear o EDT, congelar o modal e induzir o operador a repetir o clique durante o
checkout.

## Decisão

- O botão `Verificar Vale` usa `UIHelper.submitAsync`, fica desactivado durante a consulta e apresenta
  o estado `A VALIDAR VALE...`.
- A confirmação usa `ModernFormDialog.setOnSaveAsync`: a fábrica captura e valida campos no EDT e a
  tarefa devolvida executa apenas a consulta remota.
- A regra de validade, estado activo e saldo suficiente permanece centralizada em
  `PosPaymentDialog.validateVoucher` e é aplicada tanto na verificação como na confirmação.
- Uma falha mantém o modal aberto, reactiva os controlos e mostra uma mensagem accionável em pt-MZ.

## Critérios de aceitação

1. Nenhuma chamada `POSApiClient.getVoucher` é executada directamente por um listener ou `setOnSave`.
2. Cliques repetidos não criam pedidos concorrentes pelo mesmo botão.
3. Vale inexistente, expirado, utilizado ou com saldo insuficiente continua rejeitado.
4. `PosVoucherUiHarnessTest` e `DesktopThinContextTest` passam.

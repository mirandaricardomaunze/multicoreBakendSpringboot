# Spec — Actualização multiutilizador no fluxo comercial

**Data:** 2026-08-17

## Objectivo

Permitir que operadores da mesma loja carreguem explicitamente o estado mais recente do backend
antes de uma decisão, mantendo também a recarga automática depois de operações concluídas.

## Regras

1. POS, Facturação, Pedidos, Guias, Recibos, Notas e Movimentos apresentam um botão visível
   `Actualizar`, com ícone, tooltip e nome acessível canónicos.
2. O POS recarrega sessão de caixa, clientes, produtos, armazéns, contas e histórico visível.
3. Facturação e Pedidos recarregam as respectivas listagens; a paginação regressa ao estado canónico.
4. Guias e Notas recarregam a listagem activa directamente do backend.
5. Operações de escrita continuam a recarregar automaticamente as áreas afectadas após sucesso.
6. A recarga é assíncrona e não substitui transacções, idempotência, reservas ou controlo concorrente.


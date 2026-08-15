# Limite de crédito do cliente — especificação

**Criado em:** 2026-08-14
**Estado:** implementado (backend + desktop), automatizado em LC-01..LC-32
**Origem:** lacuna levantada na auditoria de gestão de 2026-08-09 (`tasks/current.md`): *"sem
limite de crédito do cliente"*.

---

## 1. O problema

Com a V35 o sistema passou a saber quanto cada cliente deve e há quanto tempo. Continuava a não
ter **trava nenhuma**: qualquer cliente podia continuar a levar fiado indefinidamente, no balcão
e na faturação, sem que nada avisasse. O gestor só descobria o buraco quando ia cobrar.

Numa loja isto é o modo normal de perder dinheiro: o fiado cresce um pouco de cada vez, sempre
autorizado por quem está ao balcão e nunca por quem responde pelo caixa.

---

## 2. A regra canónica

### `Client.creditLimit` — três estados, não dois
| Valor | Significado |
|---|---|
| `null` | **Sem limite** — crédito livre. É o comportamento de toda a base anterior à V36. |
| `0.00` | **Não vende fiado** a este cliente. |
| `> 0` | Tecto de dívida em aberto. |

Nulo e zero são coisas diferentes; tratá-los como iguais era o erro fácil aqui.

### `Client.exceedsCreditLimit(dividaActual, dividaNova)`
Fonte única da decisão. Recusa quando `dívidaActual + dívidaNova > limite`.

- O limite é **tecto inclusivo**: chegar exactamente ao limite ainda passa; um cêntimo acima
  recusa.
- `dívidaNova ≤ 0` **passa sempre**: uma venda paga na hora não consome crédito, mesmo que o
  cliente já esteja estourado. Quem paga a pronto não devia ser impedido de comprar.
- Sem limite definido, passa sempre.

### `Client.creditAvailable(dividaActual)`
Quanto ainda pode levar. `null` sem limite definido; nunca negativo (quem estourou tem zero).

---

## 3. Onde a trava é aplicada

`ReceivablesService.assertCreditAvailable(client, dividaNova)` junta a dívida actual
(`outstandingTotalFor`, soma dos saldos das faturas **cobráveis** do cliente na empresa activa)
à aritmética do domínio, e recusa com uma mensagem que diz os números:

> Limite de crédito excedido para Loja Central. Limite: 10000.00 MT · Em dívida: 8000.00 MT ·
> Disponível: 2000.00 MT · Esta venda a crédito: 3000.00 MT.

As **três portas que criam dívida** chamam-na:

| Porta | Dívida nova |
|---|---|
| `ComercialService.createInvoice` | o **total** da fatura (nasce por receber) |
| `ComercialService.billOrder` | o total da encomenda facturada |
| `POSService.checkout` | `total − pago agora` (só a parte a fiado) |

### Ordem: verificar antes de mexer em nada
No POS, a saída de stock estava **dentro** do ciclo das linhas, ou seja, acontecia antes de a
venda estar autorizada. Passou para `deductStockForSale(...)`, chamado **depois** da trava de
crédito. A transacção reverteria de qualquer forma, mas depender do rollback para não fazer
estragos é frágil — a ordem é que tem de estar certa. Na faturação, a verificação vem **antes**
de `documentNumberService.next(...)`: uma recusa não pode consumir um número da série FT e abrir
um salto que a AT não admite.

---

## 4. O que a trava **não** faz (v1)

- Não bloqueia por **atraso** (só por valor). Um cliente com 100 MT vencidos há um ano continua
  a poder comprar se tiver limite disponível. O bloqueio por antiguidade é matéria da spec de
  [vencimento](VENCIMENTO_ANTIGUIDADE_SPEC.md) e fica para v2.
- Não há **autorização de excepção** por gerente (aprovar uma venda acima do limite). Hoje é
  recusa dura; a Engine de Aprovações é o sítio natural para isso, se se quiser.
- O limite é por cliente, não por grupo de empresas do mesmo dono.

---

## 5. Migração V36

```sql
alter table clients add column if not exists credit_limit numeric(14, 2);
```

Sem `default`: todos os clientes existentes ficam a `null` = sem limite. **Nenhum cliente muda
de regra** sem o gestor a definir explicitamente.

# Recebimentos e saldo em dívida — especificação

**Criado em:** 2026-08-09
**Estado:** implementado (backend + desktop), automatizado em RP-01..RP-23
**Origem:** auditoria a pedido do utilizador ("o sistema está preparado para gestão?")

---

## 1. O problema

O mesmo conceito — *quanto é que o cliente ainda deve* — estava implementado três vezes, de
três maneiras, com resultados diferentes.

| Porta | Onde | O que fazia |
|---|---|---|
| POS | `POSService.deriveStatus` / `settleCredit` | **Certo.** Comparava pago com total, usava `PARTIALLY_PAID`, validava contra o saldo. |
| Faturação | `ComercialService.createReceipt` | **Errado.** Marcava `PAID` por qualquer valor e nunca acumulava `amountPaid`. |
| Tesouraria | `FinanceService.payInvoice` | **Errado.** Sem guarda de papel, registava sempre o total mesmo que já houvesse recebimentos. |

E do lado da leitura, o mesmo outra vez:

| Ecrã | Definição de "por cobrar" |
|---|---|
| Contas Correntes (`ComercialService.getOutstandingInvoicesByCompany`) | `APPROVED` + `PARTIALLY_PAID`, saldo em dívida — **certo** |
| Dashboard (`ReportService.unpaidInvoicesTotal`) | só `APPROVED`, valor **total** — **errado** |

**Consequência de negócio:** um cliente que pagasse 100 de uma fatura de 232 ficava com a
fatura *Paga*. Os 132 restantes desapareciam das contas correntes, do dashboard e de qualquer
hipótese de cobrança. É a mesma forma do bug do IVA fechado a 2026-08-06 — **a mesma regra em
duas portas, a divergir em silêncio**.

---

## 2. A regra canónica

Fonte única no domínio, mesmo padrão de `Product.effectiveTaxRate()`.

### `Invoice.outstandingAmount()`
`total − recebido`, nunca negativo. **É este** o valor que o cliente deve — nunca o total.

### `Invoice.deriveStatusFromPayments()`
| Condição | Estado |
|---|---|
| `pago ≥ total` | `PAID` |
| `pago > 0` | `PARTIALLY_PAID` |
| `pago = 0` | `APPROVED` (fiado, emitida e por receber) |

Chamar **só depois** de actualizar `amountPaid`, e **só** sobre faturas emitidas. Um documento
anulado, rejeitado ou à espera de aprovação mantém o seu estado.

### `InvoiceStatus.isRealisedSale()` — `APPROVED`, `PARTIALLY_PAID`, `PAID`
"Isto conta como venda": a mercadoria saiu e o stock baixou. Um fiado **é** uma venda; só o
recebimento é que ficou por fazer. Exclui rascunhos, documentos à espera de aprovação (o stock
ainda não se moveu), rejeitados e anulados.

### `InvoiceStatus.isCollectable()` — `APPROVED`, `PARTIALLY_PAID`
"Vale a pena perguntar se há saldo". O valor vem sempre de `outstandingAmount()`.

---

## 3. Regras de operação

1. **Recibo parcial não liquida a fatura.** `createReceipt` acumula em `amountPaid` e deriva o
   estado. Um recibo de 100 numa fatura de 232 deixa-a `PARTIALLY_PAID` com 132 por receber.
2. **Uma fatura por cobrar aceita vários recibos**, até o saldo chegar a zero.
3. **Nenhum recibo excede o saldo em dívida**, nem é aceite com valor ≤ 0. Sem isto entrava
   dinheiro a mais na tesouraria e a fatura ficava com "pago" acima do total.
4. **Anular um recibo devolve só o valor desse recibo.** Com vários recibos na mesma fatura,
   anular um não pode apagar os outros — o estado volta a derivar do que continua pago.
5. **Liquidar fatura pela tesouraria exige MANAGER/ADMIN**, como anular recibo. O módulo
   `financeira` era o único módulo de dinheiro sem `PermissionGuard`.
6. **`payInvoice` só movimenta o saldo em dívida**, nunca o total — senão o que já tinha sido
   recebido por recibo ou no POS era contado duas vezes na tesouraria.
7. **Dashboard e relatório diário contam as mesmas vendas** (`isRealisedSale`) e o mesmo por
   cobrar (`isCollectable` + `outstandingAmount`) que as Contas Correntes.

---

## 4. Fora de âmbito (deliberadamente)

- **`FiscalSalesExportService.ISSUED` não foi tocado.** Inclui `CANCELLED` de propósito: o
  SAF-T tem de reportar documentos anulados. É uma definição fiscal, não comercial — **não
  unificar** com `isRealisedSale()`.
- **Data de vencimento e aging** continuam por fazer: `Invoice` não tem `dueDate`. O sistema
  sabe *quanto* falta receber, mas não sabe *se está em atraso*.
- **Limite de crédito do cliente** continua por fazer: `Client` não tem `creditLimit`.
- Sem migração de base de dados — `amount_paid` e os estados já existiam.

---

## 5. Dados existentes

Faturas comerciais marcadas `PAID` por um recibo parcial **antes** deste fix continuam `PAID`
com `amount_paid = 0`: o valor real recebido só existe na linha do `Receipt`. Não há correcção
automática porque não é seguro presumir que o recibo cobria tudo. Verificar em produção com:

```sql
SELECT i.invoice_number, i.total_amount, i.amount_paid, SUM(r.amount_paid) AS recebido
FROM invoices i JOIN receipts r ON r.invoice_id = i.id
WHERE i.status = 'PAID' AND r.status = 'COMPLETED'
GROUP BY i.id, i.invoice_number, i.total_amount, i.amount_paid
HAVING SUM(r.amount_paid) < i.total_amount;
```

Cada linha devolvida é uma dívida que o sistema tinha dado por saldada.

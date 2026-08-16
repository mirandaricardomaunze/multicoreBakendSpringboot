# Vencimento e antiguidade de saldos — especificação

**Criado em:** 2026-08-14
**Estado:** implementado (backend + desktop), automatizado em VA-01..VA-25
**Origem:** lacuna levantada na auditoria de gestão de 2026-08-09 (`tasks/current.md`): *"sem
`dueDate`/aging — não se sabe o que está **em atraso**"*.

---

## 1. O problema

Depois do fix dos recebimentos (2026-08-09) o sistema passou a saber **quanto** cada cliente
deve. Continuava sem saber **há quanto tempo**.

Uma fatura só tinha `createdAt`. Não havia data-limite de pagamento em lado nenhum — nem no
documento, nem no cliente. Consequências:

- As Contas Correntes listavam tudo por igual: um fiado de ontem e um de há oito meses tinham
  exactamente o mesmo aspecto. Não havia por onde começar a cobrar.
- Não existia a pergunta "o que está vencido?" — logo, nenhuma resposta, nenhum relatório de
  antiguidade, nenhuma priorização.
- O dashboard dizia *por cobrar: X*, sem distinguir o que ainda está no prazo do que já rebentou
  o prazo. São coisas diferentes: uma é normal, a outra é um problema.

---

## 2. A regra canónica

Fonte única no domínio, mesmo padrão de `Product.effectiveTaxRate()` e de
`Invoice.outstandingAmount()`.

### `Client.paymentTermsDays` — o acordo
Prazo em dias, a contar da emissão. **Zero = pronto pagamento**, que é o comportamento de toda a
base anterior à V35: nenhum cliente existente muda de regra por causa desta alteração.

### `Invoice.dueDate` — a data-limite, **gravada no documento**
Não é recalculada a partir do cliente na leitura. O prazo acordado pode mudar amanhã; o
vencimento de uma fatura **já emitida** não pode. Alterar o prazo do cliente só afecta faturas
futuras.

### `Invoice.assignDueDate(issueDate, explicit)`
A única porta que decide um vencimento. Chamada pelas **três** portas que criam faturas:
`ComercialService.createInvoice`, `ComercialService.billOrder` e `POSService.checkout`.

| Entrada | Vencimento |
|---|---|
| `explicit` preenchido | essa data |
| `explicit` vazio | `issueDate + client.effectivePaymentTermsDays()` |
| `explicit` anterior à emissão | **recusa** (`BusinessRuleException`) |

### `Invoice.effectiveDueDate()`
`dueDate`, senão a data de emissão. Cobre os documentos anteriores à V35 (leitura conservadora:
uma dívida antiga sem prazo gravado conta como vencida desde a emissão, nunca como "ainda no
prazo").

### `Invoice.daysOverdue(today)`
Dias a contar do vencimento. Vale **zero** — não negativo, não nulo — quando:
- a fatura não é cobrável (`isCollectable()` falso: paga, anulada, rejeitada, rascunho); **ou**
- o saldo (`outstandingAmount()`) é zero; **ou**
- `today` ainda não passou de `effectiveDueDate()`.

**O dia do vencimento não é atraso.** Quem vence hoje tem o dia todo para pagar; o atraso começa
no dia seguinte.

### `AgingBucket` — a escala
| Escalão | Dias de atraso | Rótulo |
|---|---|---|
| `CORRENTE` | ≤ 0 | Corrente (por vencer) |
| `ATE_30` | 1–30 | 1–30 dias |
| `DE_31_A_60` | 31–60 | 31–60 dias |
| `DE_61_A_90` | 61–90 | 61–90 dias |
| `MAIS_DE_90` | > 90 | Mais de 90 dias |

`AgingBucket.of(dias)` é a única implementação dos cortes; `isOverdue()` é tudo menos
`CORRENTE`. Os cortes 30/60/90 são a convenção comercial corrente em Moçambique.

---

## 3. Contas a receber — `ReceivablesService`

Serviço próprio (o `ComercialService` já passa das 1.100 linhas): a cobrança **lê** faturas, não
as emite — responsabilidade autónoma (SRP). Não reimplementa nenhuma regra: saldo, atraso e
escalão vêm todos do domínio.

`GET /api/comercial/receivables/aging?reference=yyyy-MM-dd` → `AgingSummaryDTO`:

- `buckets` — os **cinco** escalões, sempre presentes mesmo a zero (uma tabela que perde linhas
  quando estão vazias faz o leitor duvidar do relatório).
- `clients` — repartição por cliente, do maior devedor para o menor, com `oldestDueDate` e
  `maxDaysOverdue`.
- `total` / `overdueTotal` — tudo por receber vs. só o que passou do prazo.

A data de referência é **parâmetro**, não `now()` lá dentro: o relatório é reproduzível e
testável sem mexer no relógio do sistema.

---

## 4. Alterações de leitura já existentes

`ComercialService.getOutstandingInvoicesByCompany` repetia a comparação de estados e de valores
à mão — a **terceira** cópia da mesma regra. Passou a usar `isCollectable()` +
`outstandingAmount()` e a devolver a lista **ordenada pelo maior atraso**: a cobrança começa por
cima.

---

## 5. Migração V35

```sql
alter table clients  add column if not exists payment_terms_days integer not null default 0;
alter table invoices add column if not exists due_date date;
update invoices set due_date = cast(created_at as date) where due_date is null;
```

Retroactivo conservador: documentos anteriores ficam com vencimento = data de emissão.

---

## 6. Limites conhecidos (v1)

- O **limite de crédito** do cliente é matéria separada (spec própria) — esta só responde
  "há quanto tempo", não "pode continuar a comprar fiado".
- Sem juros de mora nem notas de aviso automáticas.
- O escalonamento é por documento, não por linha de documento.

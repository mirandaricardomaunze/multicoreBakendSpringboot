# Encomendas: duas vias declaradas (A4 profissional · pedido térmico)

**Estado:** em construção · **Data:** 2026-08-18
**Harness:** [ENCOMENDA_DUAS_VIAS_HARNESS.md](ENCOMENDA_DUAS_VIAS_HARNESS.md) (ED-01..ED-24 automáticos, ED-50..ED-58 manuais)
**Canónico afectado:** [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md)

---

## 1. Problema

O sistema já tem **dois circuitos de encomenda**, construídos em alturas diferentes:

| | Encomenda formal | Pedido de separação |
|---|---|---|
| Nasce em | `ComercialService.createOrder` | `CustomerOrderFulfillmentService.submit` |
| Aprovação | `PENDING_APPROVAL` → motor de aprovações → `PENDING` | nenhuma — a aprovação criada é **cancelada** logo a seguir |
| Documento | A4 (`OrderPrintService`) | talão térmico (`OrderPickingPrintService`) |
| Percurso | fatura-se (ou converte-se em guia) | separa-se no armazém e só depois se fatura |

**Os dois circuitos existem e funcionam. O que não existe é o sistema saber qual é qual.**

Três defeitos concretos daí resultantes:

### 1.1 O tipo é adivinhado a partir do estado

`CustomerOrderFulfillmentActions.printSelected` escolhe o documento assim:

```java
if ("AWAITING_SEPARATION".equals(status))  → guia térmica
else if ("IN_SEPARATION".equals(status))   → reimpressão autorizada
else                                        → A4
```

O `else` é o problema. Qualquer estado que não seja um dos dois cai no A4 — incluindo estados
que ainda não existem. Acrescentar um estado ao circuito de separação faz sair, em silêncio, o
documento errado. O tipo de um documento é um **facto do documento**, não uma consequência do
sítio onde ele está parado.

### 1.2 O talão térmico não tem o desenho do recibo do POS

Os dois documentos térmicos do sistema divergiram:

| | `ReceiptPrintService` (POS) | `OrderPickingPrintService` (separação) |
|---|---|---|
| Logótipo | sim | **não** |
| NUIT / morada / telefone / email | sim | **não** |
| Separadores | pontilhados | nenhum |
| Bordas da tabela | inferior pontilhada | caixa completa |
| Rodapé configurável | sim | não |

Duas impressoras térmicas na mesma loja a cuspir dois desenhos diferentes da mesma empresa.

### 1.3 O bloco do cliente está escrito duas vezes

`InvoicePrintService.buildClientBlock` e `OrderPrintService.buildClientBlock` são ~35 linhas
**idênticas**. Hoje o A4 da encomenda é igual ao da fatura por coincidência de manutenção, não
por construção. É a forma exacta do bug que este projecto já apanhou três vezes — IVA (06/08),
saldo em dívida (09/08), "isto conta como venda" — **a mesma regra em duas portas**.

---

## 2. Decisão

**O tipo passa a ser declarado na criação e gravado no documento.**

```
OrderKind
├── FORMAL_ORDER     "Encomenda (A4)"      → aprovação obrigatória · documento A4 igual à fatura
└── PICKING_REQUEST  "Pedido de separação" → sem aprovação · talão térmico igual ao recibo do POS
```

O tipo decide **três** coisas, e é a única coisa que as decide:

1. **Se precisa de aprovação** — `OrderKind.requiresApproval()`
2. **Que documento sai** — `OrderKind.isThermal()`
3. **Se entra no circuito de separação** — só `PICKING_REQUEST`

O estado deixa de decidir o que quer que seja sobre formato ou aprovação. Continua a ser o que
sempre foi: onde o documento está parado.

### 2.1 Porquê aprovação só na via A4

É o pedido explícito do utilizador (18/08) e bate certo com a operação: um pedido de separação
é trabalho interno de armazém — quem o cria já tem o cliente à frente e o stock é **reservado**,
não vendido. A saída de dinheiro/stock acontece na **facturação**, que continua a ter as suas
próprias travas (limite de crédito, stock disponível). Uma encomenda A4 é um compromisso
comercial formal com o cliente e passa pelo motor de aprovações existente: automático até 50 MT,
gerente até 500 MT, administrador acima disso.

Consequência: `CustomerOrderFulfillmentService.submit` deixa de **criar e depois cancelar** um
pedido de aprovação. Não cria nenhum. A chamada a `cancelPendingForDocument` deixa de fazer
sentido no caminho normal e sai.

---

## 3. Desenho dos documentos

### 3.1 Um desenho por família, partilhado por construção

| Família | Peça partilhada (nova) | Quem a usa |
|---|---|---|
| A4 | `ClientBlockRenderer` | `InvoicePrintService`, `OrderPrintService` |
| Térmica | `ThermalReceiptRenderer` | `ReceiptPrintService`, `OrderPickingPrintService` |

`ThermalReceiptRenderer` centraliza o que fazia do recibo do POS um recibo: logótipo escalado,
nome, NUIT, morada, telefone, email, separador pontilhado, borda inferior pontilhada das células
e os alinhamentos. À prova de falha, como já era: sem logótipo ou com logótipo ilegível, o talão
sai na mesma.

**O que não muda:** o recibo do POS mantém-se pixel a pixel como está. A extracção é para o
talão de separação **passar a ser como ele**, não o contrário. `ReceiptPrintServiceTest` é a
rede que garante isso.

### 3.2 O que cada documento continua a ter de próprio

- **Talão de separação:** título "GUIA DE SEPARACAO" (ou "REIMPRESSAO — …"), armazém, colunas
  Artigo/Qtd./Peso, peso bruto total e as duas linhas de assinatura (separado por / conferido por).
- **Recibo do POS:** operador, bloco de pagamentos (método, valor entregue, troco) e rodapé
  configurável.
- **Encomenda A4:** peso bruto total e o estado; a fatura não tem peso.

---

## 4. Regras

| # | Regra |
|---|---|
| R1 | Toda a encomenda tem um `kind` não nulo. Sem indicação, `FORMAL_ORDER`. |
| R2 | `FORMAL_ORDER` nasce em `PENDING_APPROVAL` e submete pedido de aprovação. |
| R3 | `PICKING_REQUEST` **não** cria pedido de aprovação nenhum. |
| R4 | `printForPicking`/`reprint`/`completeSeparation` recusam uma `FORMAL_ORDER`, dizendo porquê. |
| R5 | A impressão escolhe o documento pelo `kind`. O estado não entra nessa decisão. |
| R6 | O `kind` é imutável depois de criado. Um pedido não vira encomenda nem vice-versa. |
| R7 | O talão de separação e o recibo do POS partilham cabeçalho e estilo por construção. |
| R8 | A encomenda A4 e a fatura partilham o bloco do cliente por construção. |

---

## 5. Migração V43 — retroactiva conservadora

Coluna `kind varchar(20) not null default 'FORMAL_ORDER'`.

O backfill não adivinha pelo estado, que é ambíguo (`CANCELLED` existe nos dois circuitos). Usa
o marcador preciso: **o circuito de separação é o único que grava `idempotency_key`**
(`createAndReserve`). Os estados de separação entram como rede de segurança.

```sql
update customer_orders set kind = 'PICKING_REQUEST'
 where idempotency_key is not null
    or status in ('AWAITING_SEPARATION','IN_SEPARATION','SEPARATED');
```

Documentos já emitidos continuam a comportar-se exactamente como se comportavam. Nenhum muda de
via por causa desta migração.

---

## 6. Fronteira HTTP

- `CreateOrderRequest` ganha `kind` **opcional** (construtor retrocompatível — ausente = `FORMAL_ORDER`).
- `OrderDTO` ganha `kind` e `kindLabel` (rótulo PT-MZ, para a tabela não traduzir estados por conta própria).
- O caminho de expedição **força** `PICKING_REQUEST` no servidor, ignorando o que venha no pedido.
  Confiar no cliente para declarar o tipo seria a mesma porta aberta que o campo `taxRate` foi até 06/08.

---

## 7. Limites declarados (v1)

- **Não** há conversão entre vias. R6 é uma decisão, não uma limitação temporária: as duas vias
  têm circuitos de stock diferentes (reserva vs. dedução na facturação) e converter a meio
  deixaria reservas órfãs.
- Os limiares de aprovação (50 / 500 MT) continuam **fixos no código**, como já estavam. Torná-los
  configuráveis por empresa é trabalho à parte.
- O talão de separação continua a sair em PDF de 80 mm e a abrir no leitor do sistema; não fala
  directamente com a impressora térmica.

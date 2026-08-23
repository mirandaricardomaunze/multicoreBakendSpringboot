# SPEC — Encomenda profissional (a que nasce da cotação)

**Criado em:** 2026-08-20
**Módulo:** `comercial`
**Migração:** `V45__order_commercial_terms.sql` (+ `delivery_days` na `V44`, ainda não aplicada)

> Continuação directa de [COTACAO_SPEC.md](COTACAO_SPEC.md). Aquela spec fez a cotação converter-se
> em encomenda; esta trata do **documento que sai dessa conversão**.

---

## 1. O problema

A conversão produzia uma encomenda **muda**. Comparada com a proposta que lhe deu origem, perdia
tudo o que fazia dela um acordo:

| A cotação dizia…                        | A encomenda gerada…                        |
|-----------------------------------------|--------------------------------------------|
| "proposta CT-2026/3"                     | não sabia que veio de lado nenhum          |
| "pagamento: 30 dias"                     | esquecia                                   |
| "entrega: 7 dias úteis após confirmação" | esquecia — e nunca havia uma **data**      |
| —                                        | imprimia `Estado: PENDING_APPROVAL`        |

O cliente confirmava uma proposta e recebia de volta um papel que não a mencionava, sem as condições
que tinha negociado, sem data de entrega, e com um código interno inglês no fim. As condições
existiam — no documento anterior, fora do alcance de quem executa a encomenda.

---

## 2. Regras

### P1 — A encomenda declara de onde veio

`Order.quotationId` + `Order.quotationNumber`, gravados na conversão e impressos ("Origem: Cotação
CT-2026/3"). A ligação era **de sentido único**: a cotação guardava a encomenda, a encomenda não
guardava a cotação. Quem abria a encomenda — armazém, aprovador, cobrança — não tinha como chegar à
proposta que a justificou.

### P2 — As condições acordadas viajam com o compromisso

`paymentTerms` e `deliveryTerms` são copiados da cotação para a encomenda. **Copiados, não
consultados por referência**: é a mesma regra da linha de preço (R2 da COTACAO_SPEC). Editar a
cotação amanhã não pode reescrever as condições de um compromisso já assumido — e a cotação, sendo
terminal depois de convertida, nem sequer se edita.

### P3 — A data de entrega nasce na confirmação e fica gravada

A cotação promete **dias** (`deliveryDays`: "7 dias úteis **após confirmação**"). A **data** só pode
ser calculada quando a confirmação acontece — que é a conversão. A cotação não a pode saber: não
sabe quando é que o cliente vai dizer que sim.

```
Quotation.deliveryDays = 7   ──conversão em 20/08──▶   Order.expectedDeliveryDate = 27/08/2026
                                                        (gravada; não recalcula)
```

Regra no domínio, molde do `Invoice.assignDueDate` (V35):

```java
order.assignExpectedDelivery(confirmedOn, days)   // days nulo → sem data prometida
```

Gravada no documento pela mesma razão do vencimento da fatura: mudar o prazo acordado com o cliente
amanhã não pode alterar a data que já foi prometida ontem.

`deliveryTerms` (texto livre) **mantém-se ao lado dos dias** e não é redundante: nem toda a promessa
de entrega é um número ("entrega faseada", "levantamento no armazém"). Os dias são o que o sistema
consegue calcular; o texto é o que o cliente leu.

### P4 — O documento não mostra códigos internos

O A4 imprimia `Estado: PENDING_APPROVAL`. Passa a imprimir "Pendente de aprovação", por
`OrderStatusLabel` — **fonte única** em PT-MZ. É a regra que a
[ENCOMENDA_DUAS_VIAS_SPEC](ENCOMENDA_DUAS_VIAS_SPEC.md) (ED-04) já tinha imposto aos ecrãs e que o
documento impresso escapava.

A tradução existia, **privada e incompleta**, dentro do `CustomerOrderFulfillmentService` (cobria os
estados de separação e ignorava `PENDING_APPROVAL`, `PENDING`, `BILLED`, `GUIDE_PENDING`,
`GUIDED`). Passa a delegar na fonte única, em vez de manter a segunda cópia.

**Divergência que fica em aberto:** o `UIHelper.humanStatus` — tradutor **genérico** usado pelo
renderer das tabelas, partilhado com faturas, notas e guias — diz "Pendente" onde o
`OrderStatusLabel` diz "Pendente de aprovação". A mesma encomenda mostra os dois rótulos consoante
o sítio. Não é contradição (é diferença de precisão), mas é a mesma regra em duas portas. Não se
resolve fazendo o `humanStatus` delegar: ele traduz estados que o `OrderStatusLabel` não conhece
(`PAID`, `APPROVED`, `PARTIALLY_PAID`, …). Ver §5.

### P5 — O desenho é partilhado por construção, não copiado

O bloco de condições e o bloco de assinaturas passam a viver em renderizadores próprios,
usados por **todos** os documentos que os têm:

| Renderizador              | Usado por                                              |
|---------------------------|--------------------------------------------------------|
| `CommercialTermsRenderer` | Encomenda A4, Cotação                                  |
| `SignatureBlockRenderer`  | Encomenda A4, Cotação, Guia de Remessa                  |

Antes desta iteração o `signatureCell` estava escrito **duas vezes** nestes documentos (guia e
cotação) e ia ser escrito uma terceira. É a forma exacta do defeito que o [ClientBlockRenderer](../src/main/java/mz/multicore/erp/modules/printing/ClientBlockRenderer.java)
já corrigiu uma vez: dois documentos iguais por coincidência de manutenção, não por construção.

**O problema é maior do que esta alteração e fica declarado:** ao verificar a extracção contaram-se
**mais 10 cópias byte-a-byte iguais** do mesmo `signatureCell`, em `Payslip`, `StockTransfer`,
`CreditNote`, `DebitNote`, `GuideRemittance`, `InventoryCountSheet`, `InventoryReport`,
`IvaDeclaration`, `PayrollFiscalMap` e `POSZReport`. O `SignatureBlockRenderer` já as serve a todas
— falta migrá-las. Não foram tocadas aqui por serem documentos de salários, stock e fiscal, fora do
âmbito da encomenda profissional; ver §5.

Os blocos partilhados que a encomenda A4 já usava (cabeçalho, cliente, linhas, totais) **não mudam**
— ED-51 continua a valer: lado a lado com uma fatura, são indistinguíveis.

### P6 — Nada disto é obrigatório

Todos os campos novos são **nullable** e todos os blocos novos do PDF **só saem quando preenchidos**.

- Encomendas criadas à mão continuam a poder não ter origem, condições nem data.
- Encomendas **anteriores** a esta versão têm tudo a nulo e imprimem exactamente como antes.
- O circuito de separação (`PICKING_REQUEST`) não é tocado: continua a imprimir talão térmico.

---

## 3. Camadas

| Camada     | Ficheiro |
|------------|----------|
| model      | `Order` (+5 campos, `assignExpectedDelivery`, `isDeliveryOverdue`), `OrderTerms` (value object), `OrderStatusLabel`, `Quotation.deliveryDays` + `agreedTerms()` |
| dto        | `OrderDTO` (+origem, condições, entrega, atraso), `CreateOrderRequest` (+condições opcionais), `CreateQuotationRequest`/`QuotationDTO` (+`deliveryDays`) |
| service    | `ComercialService.placeOrder(..., OrderTerms)`; `QuotationService.convert` passa `quotation.agreedTerms()` |
| printing   | `CommercialTermsRenderer`, `SignatureBlockRenderer` (novos, partilhados); `OrderPrintService`, `QuotationPrintService`, `DeliveryGuidePrintService` adoptam-nos |
| migração   | `V45__order_commercial_terms.sql`; `delivery_days` acrescentado à `V44` |

### Porque a `V44` é editada em vez de haver uma `V45` para as cotações

A `V44` **ainda não foi aplicada em lado nenhum** — em dev o Flyway está desligado e nunca correu em
produção; está no mesmo conjunto de alterações por commitar. Acrescentar `delivery_days` a uma tabela
criada há minutos, por migração separada, deixaria a definição da `quotations` espalhada por dois
ficheiros sem qualquer ganho. A `V45` existe porque `customer_orders` é uma tabela **antiga e com
dados** — essa tem mesmo de ser alterada por migração própria.

### `OrderTerms` — porquê um objecto e não mais cinco parâmetros

`placeOrder` já recebia seis argumentos. As cinco coisas novas (origem + condições + dias) andam
sempre juntas e vêm sempre da mesma decisão comercial, pelo que viajam como **um** valor:

```java
public record OrderTerms(Long quotationId, String quotationNumber,
                          String paymentTerms, String deliveryTerms, Integer deliveryDays)
```

`OrderTerms.none()` é o caso da encomenda sem acordo prévio. `Quotation.agreedTerms()` é o caso da
conversão — **a cotação é quem sabe o que prometeu**, por isso é ela que o diz.

---

## 4. PDF da encomenda A4 — o que muda

```
┌ cabeçalho da empresa ─────────────────── ENCOMENDA · EC-2026/3 ┐  (inalterado)
│ Cliente / NUIT / morada          Data · Armazém                │  (inalterado)
│ Origem: Cotação CT-2026/3 de 20/08/2026                        │  ← novo, só se houver
│ [ linhas ]                                                      │  (inalterado)
│ Peso bruto total                                                │  (inalterado)
│ Subtotal / IVA / TOTAL                                          │  (inalterado)
│ Condições: Pagamento · Prazo de entrega · Entrega prevista      │  ← novo, só se houver
│ ____________       ____________                                 │  ← novo
│ Pela empresa       Confirmação do cliente                       │
│ Estado: Pendente de aprovação                                   │  ← era PENDING_APPROVAL
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Fora de âmbito (v1)

1. **Editar as condições da encomenda depois de criada.** Herda-se da cotação ou indica-se na
   criação; alterar depois fica para quem precisar.
2. **Alertas de entrega em atraso.** `isDeliveryOverdue` existe e vai no DTO (a data prometida que
   ninguém verifica é teatro), mas não há notificação nem relatório — só a coluna no ecrã.
3. **Data de entrega por linha.** A promessa é do documento, não do artigo.
4. **Recalcular a entrega quando a aprovação demora.** A data conta da **confirmação** (conversão),
   não da aprovação interna. Se a aprovação atrasar, a data prometida não se mexe — é isso que foi
   prometido ao cliente.
5. **Migrar as outras 10 cópias do `signatureCell`** (salários, stock, fiscal) para o
   `SignatureBlockRenderer`. Mecânico e sem risco de desenho — as cópias são idênticas — mas são
   documentos fora do âmbito desta alteração. Ver §P5.
6. **Unificar `UIHelper.humanStatus` com `OrderStatusLabel`.** Exige decidir o que fazer aos estados
   que só o tradutor genérico conhece, e toca em todas as tabelas do sistema. Ver §P4.

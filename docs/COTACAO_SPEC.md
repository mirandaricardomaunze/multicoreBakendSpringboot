# SPEC — Cotação ao cliente (proposta comercial)

**Criado em:** 2026-08-19
**Módulo:** `comercial`
**Série nova:** `CT` (cotação)
**Migração:** `V44__quotation.sql`

> Fonte de verdade sobre a **cotação**. Canónico complementar:
> [MOVIMENTOS_COMERCIAIS.md](../MOVIMENTOS_COMERCIAIS.md) (§2 documentos, §4 movimentos).

---

## 1. Objectivo

Dar ao negócio o documento que falta **antes** da encomenda: a **proposta de preço** que se envia
ao cliente, com validade, condições de pagamento e prazo de entrega — e que, quando o cliente
aceita, se converte na encomenda **sem que o preço proposto mude**.

Até aqui o sistema começava na encomenda. Quem cotava fazia-o fora do sistema (folha de cálculo,
papel), e o preço tinha de ser reintroduzido à mão na encomenda. Duas consequências: a proposta não
existia como documento (não se sabia o que se prometeu, a quem, nem até quando), e o preço
reintroduzido podia não ser o cotado.

---

## 2. O que a cotação **não** é

**R1 — A cotação não move nada.** Não baixa stock, não reserva stock, não cria dívida, não toca na
tesouraria nem na contabilidade. É uma proposta: o compromisso nasce na conversão, e é a encomenda
(e depois a fatura) que carrega os movimentos, pelas portas que já existem.

Consequência prática: uma cotação pode ser emitida por qualquer utilizador da empresa. Não há aqui
dinheiro a guardar — o `PermissionGuard` entra apenas onde há (ver §7).

---

## 3. Regra central — o preço cotado é o preço honrado

**R2.** A conversão copia as linhas da cotação **verbatim** (preço unitário, taxa de IVA, desconto,
totais). Não volta a consultar o catálogo.

É a mesma forma que o projecto já usa nas outras duas conversões:

| Conversão                          | Preço da linha vem de… |
|------------------------------------|------------------------|
| Encomenda → Fatura (`billOrder`)   | linha da **encomenda** |
| Encomenda → Guia (`createFromOrder`)| linha da **encomenda** |
| **Cotação → Encomenda** (nova)     | linha da **cotação**   |

Se a conversão reapreçasse pelo catálogo, um aumento de preço do fornecedor na segunda-feira mudava
sozinho a proposta que o cliente assinou na sexta — exactamente o defeito que
[MARGEM_CUSTO_HISTORICO_SPEC](MARGEM_CUSTO_HISTORICO_SPEC.md) fechou na margem e
[IVA_TAXA_CANONICA_SPEC](IVA_TAXA_CANONICA_SPEC.md) fechou no IVA.

**A porta continua fechada a quem chama.** O preço não vem do pedido HTTP: vem de um **documento
persistido**, cujas linhas foram apreçadas na criação pelo caminho canónico
(`Product.effectiveUnitPrice` + `Product.effectiveTaxRate`). Por isso o
`CreateQuotationLineRequest` **não tem campo de preço nem de taxa** — não há campo para ignorar,
que foi o que o `taxRate` do `CreateInvoiceLineRequest` foi até 2026-08-06.

---

## 4. Validade

**R3 — A validade é gravada no documento.** `Quotation.validUntil` é uma **data**, calculada na
emissão a partir dos dias de validade pedidos (default `QuotationValidity.DEFAULT_DAYS = 30`).
Mudar o default amanhã não altera cotações já emitidas — a mesma lição do
`Invoice.dueDate` (V35): uma promessa não muda de prazo por o acordo ter mudado hoje.

**R4 — Cotação expirada não converte.** A regra vive no domínio, num só sítio:

```java
quotation.isExpired(today)   // validUntil != null && today.isAfter(validUntil)
```

Nem o painel nem o controller repetem a comparação. Para converter uma cotação expirada é preciso
**estender a validade explicitamente** — acto separado, com permissão e auditoria (§7). Não há
extensão implícita: reviver um preço antigo é uma decisão comercial, não um efeito secundário de
carregar num botão.

O estado `EXPIRED` **não é gravado**. É derivado da data, e vai no DTO como `expired` +
`daysUntilExpiry`. Assim não é preciso agendador nocturno a passear pela tabela, não há linhas
"expiradas" desactualizadas, e estender a validade faz a cotação voltar a estar válida sem qualquer
dança de estados.

---

## 5. Máquina de estados

### `QuotationStatus` (gravado)

```
DRAFT ──send──▶ SENT ──accept──▶ ACCEPTED
  │              │                  │
  ├──────────────┴─── convert ──────┴──▶ CONVERTED   (terminal; gera encomenda EC)
  │              │
  │              └──reject──▶ REJECTED               (terminal; motivo obrigatório)
  └──cancel───────────────────▶ CANCELLED            (terminal)
```

A **validade é ortogonal** ao estado: qualquer estado aberto pode estar expirado, e expirado
bloqueia apenas a conversão.

### Porque `convert` aceita `DRAFT`, `SENT` **e** `ACCEPTED`

`send`/`accept` são **registo**, não cerimónia obrigatória. O que a cotação tem de proteger é o
dinheiro — e o dinheiro está em duas recusas, não no número de cliques:

1. não honrar um preço expirado (R4);
2. não converter a mesma proposta duas vezes (R5).

Uma loja que aceite a proposta ao balcão converte directamente; uma que trabalhe por email regista
o envio e a aceitação e tem o rasto (`sentAt`, `decidedAt`, `decidedBy`). Converter a partir de
`DRAFT`/`SENT` carimba a aceitação na mesma — nunca se perde o "quando é que o cliente disse sim".

Converter a partir de `REJECTED`, `CANCELLED` ou `CONVERTED` é recusado.

**R5 — Converte uma só vez.** Mesma decisão dos "caminhos separados" da Guia de Remessa: a cotação
convertida fica **terminal** e guarda `orderId`/`orderNumber`. Converter duas vezes criava duas
encomendas para o mesmo compromisso, e o cliente recebia (e pagava) a mercadoria a dobrar.

---

## 6. A conversão gera uma **encomenda**, não uma fatura

**R6.** `convert` produz uma `Order` do tipo `OrderKind.FORMAL_ORDER`, pela porta que já existe.

Porquê não faturar directamente a partir da cotação: a fatura tem quatro coisas que só o caminho
existente sabe fazer — baixar stock (FEFO), travar o limite de crédito, atribuir o vencimento e
numerar a série fiscal `FT` sem saltos. Uma segunda porta para faturar seria **a mesma regra em
dois sítios**, que é a forma exacta dos três bugs mais caros deste projecto (IVA, saldo em dívida,
margem). Para faturar uma cotação: converte-se em encomenda e fatura-se a encomenda —
`billOrder` inalterado.

A encomenda gerada é `FORMAL_ORDER`, logo **nasce `PENDING_APPROVAL` e passa pelo motor de
aprovações** por valor, como qualquer outra encomenda formal. O cliente ter aceitado a proposta não
substitui a aprovação interna: são duas decisões diferentes, de dois lados da mesa.

### Uma só porta para criar encomendas

Para que a cotação não construísse a sua própria `Order` (numeração, estado inicial e submissão a
aprovações duplicados), o núcleo de `ComercialService.createOrder` foi extraído:

```
createOrder(request, kind)  ── apreça pelo catálogo ──┐
                                                       ├──▶ placeOrder(company, client, warehouse,
QuotationService.convert()  ── herda da cotação ───────┘        walkInName, lines, kind)
```

`placeOrder` é o único sítio onde uma encomenda é numerada (série `EC`), recebe o estado inicial
segundo a via e é submetida à Engine de Aprovações.

---

## 7. Permissões e auditoria

| Acção                    | Permissão                        | Porquê |
|--------------------------|----------------------------------|--------|
| Criar / enviar           | utilizador da empresa (tenant)   | não move dinheiro nem stock (R1) |
| Aceitar / recusar        | utilizador da empresa (tenant)   | é registo da decisão **do cliente** |
| Cancelar                 | utilizador da empresa (tenant)   | fecha um documento sem efeitos |
| **Estender validade**    | **`MANAGER`/`ADMIN`**            | revive um preço já caducado — é uma concessão comercial |
| Converter                | utilizador da empresa (tenant)   | o gate de dinheiro é a aprovação da encomenda gerada (R6) |

Auditoria (`AuditLogService.logCurrent`): `QUOTATION_CREATE`, `QUOTATION_SEND`, `QUOTATION_ACCEPT`,
`QUOTATION_REJECT`, `QUOTATION_CANCEL`, `QUOTATION_EXTEND`, `QUOTATION_CONVERT`.

O `QUOTATION_EXTEND` regista a validade **antiga e a nova** — sem isso, um preço caducado revivido
seria indistinguível de um preço sempre válido.

---

## 8. Numeração — série `CT`, por empresa

- `DocumentSeries.QUOTATION = "CT"`; número por `DocumentNumberService.next(...)` (gapless por ano,
  por empresa — mecanismo `document_sequences` da V30).
- `UNIQUE(company_id, quotation_number)` (lição da V31 — nunca `UNIQUE` global no número).
- `CT` **não é série fiscal**: a AT não numera propostas. Cancelar uma cotação não abre buraco
  nenhum na `FT`.

---

## 9. Camadas (ARCHITECTURE.md)

| Camada     | Ficheiro |
|------------|----------|
| model      | `Quotation`, `QuotationLine`, `QuotationStatus`, `QuotationValidity` (comercial) |
| repository | `QuotationRepository` (finder por empresa + fetch de linhas) |
| dto        | `CreateQuotationRequest`, `CreateQuotationLineRequest`, `ExtendQuotationValidityRequest`, `QuotationDTO`, `QuotationLineDTO` |
| service    | `QuotationService` (regras + `@Transactional`) |
| controller | `QuotationController` — `/api/comercial/quotations` |
| printing   | `QuotationPrintService` — `GET /api/print/quotation/{id}` |

- DTO em toda a fronteira; nunca `@Entity` no controller.
- `BusinessRuleException` (mensagem PT-MZ) em toda a violação de regra.
- Injecção por construtor, campos `final`.

### Endpoints

| Método | Caminho | Efeito |
|--------|---------|--------|
| `GET`  | `/api/comercial/quotations?companyId=` | listagem da empresa |
| `GET`  | `/api/comercial/quotations/{id}`       | uma cotação com linhas |
| `POST` | `/api/comercial/quotations`            | cria (`DRAFT`) |
| `POST` | `/api/comercial/quotations/{id}/send`  | `→ SENT`, carimba `sentAt` |
| `POST` | `/api/comercial/quotations/{id}/accept`| `→ ACCEPTED` |
| `POST` | `/api/comercial/quotations/{id}/reject`| `→ REJECTED` (motivo obrigatório) |
| `POST` | `/api/comercial/quotations/{id}/cancel`| `→ CANCELLED` |
| `POST` | `/api/comercial/quotations/{id}/extend`| nova `validUntil` (MANAGER/ADMIN) |
| `POST` | `/api/comercial/quotations/{id}/convert`| `→ CONVERTED`, devolve `OrderDTO` |
| `GET`  | `/api/print/quotation/{id}`            | PDF A4 |

---

## 10. PDF — documento voltado para o cliente

Reutiliza os building blocks partilhados (`CompanyHeaderRenderer`, `LineItemsTableRenderer`,
`LineRowMapper`, `TotalsBlockRenderer`, `DocumentConfigService`), pelo que sai com o mesmo desenho
da fatura e da encomenda A4 — por construção, não por coincidência de manutenção.

Título **"Cotação"**, nº `CT-...`. Acrescenta o que uma proposta tem e uma fatura não:

- **Validade em destaque** ("Válida até dd/MM/yyyy"), e o aviso de caducidade quando já passou.
- **Condições**: pagamento, prazo de entrega, observações (só as preenchidas saem).
- **Aceitação do cliente**: linha de assinatura + data.
- **Nota de rodapé**: a cotação não é documento fiscal e não substitui a fatura.

**Não imprime o armazém.** O armazém existe no documento porque a conversão precisa dele, mas é
informação interna — não vai num documento que sai para o cliente. Por isso o bloco do cliente é
montado com `ClientBlockRenderer.build(client, date, null)`.

---

## 11. Vista de Movimentos

`MovimentoTipo.COTACAO` ("Cotação") entra na leitura agregada (`MovimentosService`), ao lado de
fatura, encomenda, guia e notas. A cotação **é** um movimento comercial mesmo não movendo stock:
quem procura "o que é que prometemos a este cliente" procura no mesmo sítio onde procura tudo o
resto.

---

## 12. Fora de âmbito (v1) — declarado, não esquecido

1. **Revisões da cotação** (rev. 1, rev. 2 sobre a mesma proposta). Hoje, mudar o preço faz-se com
   uma cotação nova; a antiga recusa-se ou cancela-se. A ligação entre revisões fica por fazer.
2. **Conversão parcial** (converter só algumas linhas). A conversão leva a cotação inteira, tal
   como a guia leva a encomenda inteira.
3. **Envio por email a partir do sistema.** `send` regista que foi enviada; o envio em si é feito
   pelo operador com o PDF.
4. **Reserva de stock e verificação de disponibilidade ao cotar.** Uma cotação não promete stock —
   a disponibilidade é verificada na encomenda/faturação, que é onde o stock se move.
5. **Conversão directa em fatura** — decisão de desenho, não omissão. Ver R6.

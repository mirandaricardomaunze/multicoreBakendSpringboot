# Movimentos Comerciais — Mapa Canónico

> Fonte de verdade sobre **que documentos de venda existem**, **que movimentos cada um gera**
> e **o que ainda falta**. Lê este ficheiro antes de mexer em faturação, POS, notas ou stock.
> Detalhe de camadas em [ARCHITECTURE.md](ARCHITECTURE.md); convenções em [CONVENTIONS.md](CONVENTIONS.md).

**Última actualização:** 2026-11-10 (documentos sectoriais §8, existências na contabilidade §3)

---

## 1. Resposta directa à pergunta

| Movimento pedido        | Existe? | Como está modelado                                                                 |
|-------------------------|:-------:|------------------------------------------------------------------------------------|
| **Venda POS**           | ✅      | Não é documento próprio — é uma `Invoice` com `SalesChannel.POS`, série **FT**.    |
| **Fatura**              | ✅      | `Invoice` (canais `MANUAL`, `POS`, `ORDER`), série **FT**.                          |
| **Nota de Crédito**     | ✅      | `CreditNote`, série **NC**. Devolve stock na aprovação se motivo = `RETURN`.        |
| **Guia (transferência entre armazéns)** | ✅ | `StockTransfer`, série **TRF**. Create/approve/reject/cancel com stock a sair só na aprovação; PDF via `StockTransferPrintService`. |
| **Guia de Remessa ao cliente** | ✅ | `DeliveryGuide`, série **GR**. Gerada a partir de uma encomenda; stock (SALE) sai só na aprovação; PDF via `DeliveryGuidePrintService`. |
| **Cotação / proposta**  | ✅      | `Quotation`, série **CT**. Não move nada; converte-se em encomenda herdando os preços cotados. |
| **Reposição interna**   | ✅      | `Order` com via `INTERNAL_REPLENISHMENT`. Pedido de uma loja ao armazém central; termina em `StockTransfer`, **nunca em factura**. |

> ✅ **Decisão (2026-07-23) — reverte a de 2026-06-21:** a **Guia de Remessa ao cliente passa a ser
> requisito** e está implementada (`DeliveryGuide`, série `GR`) — ver §5.1 e
> [docs/GUIA_REMESSA_ENCOMENDA_SPEC.md](docs/GUIA_REMESSA_ENCOMENDA_SPEC.md). A Guia de
> Transferência entre armazéns (`StockTransfer`, `TRF`) continua a existir para o movimento
> **entre armazéns**. São documentos distintos.

---

## 2. Documentos comerciais existentes

Todos vivem em `modules/comercial/` (excepto o ciclo de caixa, em `modules/pos/`) e numeram
pela série central [`DocumentSeries`](src/main/java/mz/multicore/erp/modules/numbering/service/DocumentSeries.java).

| Documento        | Entidade      | Série  | Tabela            | Estado / ciclo                                              |
|------------------|---------------|--------|-------------------|------------------------------------------------------------|
| Cotação          | `Quotation`   | `CT`   | `quotations`      | `DRAFT → SENT → ACCEPTED → CONVERTED` / `REJECTED` / `CANCELLED`. Caducidade **derivada** de `valid_until`, não gravada. |
| Encomenda        | `Order`       | `EC`   | `customer_orders` | `PENDING_APPROVAL → (aprovação) → PENDING → BILLED` / `CANCELLED` |
| Fatura           | `Invoice`     | `FT`   | `invoices`        | `DRAFT → PENDING_APPROVAL → APPROVED → PAID` / `CANCELLED` |
| Recibo           | `Receipt`     | `RC`   | —                 | `COMPLETED` / anulado                                      |
| Nota de Crédito  | `CreditNote`  | `NC`   | —                 | `PENDING_APPROVAL → APPROVED` / `REJECTED` / `CANCELLED`   |
| Nota de Débito   | `DebitNote`   | `ND`   | —                 | Puramente financeira (sem stock). Numeração sequencial gapless via `DocumentSeries.DEBIT_NOTE`. |
| Guia de Remessa  | `DeliveryGuide` | `GR` | `delivery_guides` | `PENDING_APPROVAL → APPROVED` / `REJECTED` / `CANCELLED`. Gerada da encomenda; stock SALE sai na aprovação. |

---

## 3. Os quatro livros de movimentos (não confundir)

O sistema **não tem um livro único de "movimentos comerciais"**. Tem quatro ledgers
independentes, cada um na sua fronteira. Um mesmo documento pode tocar vários.

| Ledger                | Entidade / Enum                              | Módulo        | O que regista                                  |
|-----------------------|----------------------------------------------|---------------|------------------------------------------------|
| **Stock**             | `StockMovement` / `StockMovementType`        | `inventory`   | `PURCHASE, ENTRY, SALE, TRANSFER, ADJUSTMENT, RETURN, REVERSAL, WASTE, PRODUCTION` |
| **Caixa (gaveta)**    | `TillMovement` / `TillMovementType`          | `pos`         | `SALE, SUPRIMENTO, SANGRIA`                    |
| **Tesouraria**        | `TreasuryTransaction` / `TransactionType`    | `financeira`  | `DEBIT` (entrada) / `CREDIT` (saída)           |
| **Contabilidade**     | `JournalEntry` + `JournalLine` / `JournalSource` | `accounting` | Partidas dobradas (PGC-NIRF), série `LC`   |

O ledger contabilístico (desde 2026-08-15) **não substitui** os outros três: é a leitura
contabilística dos mesmos factos. Alimenta-se por **eventos** (`SaleRegisteredEvent`,
`PaymentReceivedEvent`), pelo que o módulo `comercial` não conhece o `accounting`. Ver
[docs/CONTABILIDADE_SPEC.md](docs/CONTABILIDADE_SPEC.md).

**Existências na contabilidade (desde 2026-11-10):** o que muda o stock sem ser venda nem compra também é
lançado, para a conta **Mercadorias (3201)** acompanhar o stock físico (`InventoryValueChangedEvent` →
`InventoryPostingService`): quebras (`WASTE`) e acertos de inventário (`ADJUSTMENT`, incluindo contagens e granel)
ao custo do artigo; desmancha (carcaça ao valor contabilístico, cortes ao custo pago); lote do plano de produção
(ingredientes ao custo, produto ao custo total contra Variação da produção 7301); consumo de matéria-prima da ordem
de produção. Saídas: D CMVMC 6101 / C 3201; entradas: D 3201 / C contrapartida. Cada documento só se lança uma vez.
As entradas manuais (`ENTRY`) continuam sem lançamento: são saldo inicial e lançam-se à mão pelo contabilista.

Princípio em vigor (ver [POSService](src/main/java/mz/multicore/erp/modules/pos/service/POSService.java)):
numerário de venda entra **só na gaveta** durante a sessão; só chega à **tesouraria** no fecho
de caixa (depósito do líquido), evitando dupla contagem.

---

## 4. Que movimentos cada documento dispara

```
VENDA POS  (Invoice + SalesChannel.POS)         POSService.checkout()
  ├─ StockMovement  SALE      (saída, por linha)   InventoryService.registerMovement(...,"SALE")
  ├─ PaymentEntry   por método de pagamento
  ├─ TillMovement   SALE      (se numerário, em sessão)
  └─ TreasuryTransaction DEBIT (CARD/TRANSFER, ou numerário fora de sessão)

FATURA MANUAL  (Invoice, SalesChannel.MANUAL)   ComercialService.createInvoice()
  Requer perfil MANAGER/ADMIN (PermissionGuard). Faturação directa:
  ├─ SEM desconto >10%  → emite já APPROVED e baixa stock no acto:
  │     └─ StockMovement SALE (saída, por linha) em createInvoice()
  └─ COM desconto >10%  → PENDING_DISCOUNT_APPROVAL → Engine de Aprovações
        └─ ao APROVAR:  InvoiceApprovalCallback.onApproved() → StockMovement SALE
     Recibo (createReceipt) → TreasuryTransaction DEBIT

COTAÇÃO  (Quotation, série CT)                  QuotationService.create()
  └─ NENHUM movimento: sem stock, sem dívida, sem caixa, sem contabilidade. É uma proposta.
     convert() → encomenda EC herdando os preços COTADOS (não o catálogo de hoje) ⇒ cotação CONVERTED.
     Caducada não converte; estender a validade é acto de gerente, auditado com data antiga e nova.
     Converte uma só vez. Para facturar: converte-se em encomenda e factura-se a encomenda.

REPOSIÇÃO INTERNA  (Order, via INTERNAL_REPLENISHMENT)   InternalReplenishmentService
  └─ NÃO move stock e NÃO é facturável: a mercadoria muda de armazém, não sai da empresa.
     convertToTransfer() → StockTransfer (TRF) ⇒ encomenda TRANSFER_PENDING.
     Aprovar a transferência move o stock UMA vez ⇒ encomenda TRANSFERRED (terminal).
     Rejeitar/cancelar → encomenda volta a PENDING. Ligação gravada nos dois sentidos.
     Se a transferência já foi feita sem pedido, regista-se a encomenda em falta (nasce TRANSFERRED).

ENCOMENDA  (Order, série EC)                    ComercialService.createOrder()
  └─ Engine de Aprovações (documentType "ORDER", por valor): ao APROVAR,
       OrderApprovalCallback.onApproved() → estado PENDING (faturável); ao REJEITAR → CANCELLED.
     Não move stock — só a faturação o faz.

FATURA DE ENCOMENDA  (Invoice, SalesChannel.ORDER)  ComercialService.billOrder()
  └─ Só faturável quando a encomenda está PENDING (aprovada).
  └─ StockMovement SALE (saída, por linha)

GUIA DE REMESSA  (DeliveryGuide, série GR)          DeliveryGuideService.createFromOrder()
  └─ Gerada de uma encomenda PENDING → encomenda passa a GUIDE_PENDING (deixa de ser faturável).
  └─ approve() → StockMovement SALE (saída, por linha) → encomenda GUIDED (terminal).
     reject()/cancel() → sem stock → encomenda volta a PENDING (faturável).
  Faturar a guia aprovada (DeliveryGuideBillingService) não move stock; a encomenda fica BILLED.

ANULAÇÃO DE FATURA   ComercialService.cancelInvoice()
  └─ StockMovement REVERSAL (reposição de stock)
  └─ InvoiceCancelledEvent → estorno do lançamento. Venda POS: usar devolução. Com notas aprovadas: recusada.

NOTA DE CRÉDITO (motivo RETURN)   CreditNoteService.approve()
  └─ StockMovement RETURN (entrada, reposição no armazém)

NOTA DE CRÉDITO (outros motivos) / NOTA DE DÉBITO
  └─ Sem movimento de stock — só efeito documental/financeiro
```

**Nota importante sobre o timing do stock:** a fatura **POS**, a **de encomenda** e a **manual
sem desconto >10%** baixam stock **no acto** (`createInvoice` chama `registerMovement` via
`deductStockForInvoice`). Só a fatura manual **com desconto >10%** adia a baixa para a **aprovação**
(em [InvoiceApprovalCallback](src/main/java/mz/multicore/erp/modules/comercial/service/InvoiceApprovalCallback.java)).
Decisão de 2026-06-20: emitir fatura é operação directa de quem tem perfil autorizado (atribuído pelo
admin), não passa pela Engine de Aprovações — só o desconto sensível continua a exigir gerente.

---

## 4.1 Ordem de produção (desde 2026-11-10)

A conclusão de uma ordem de produção move o stock **pelo serviço central** (`PRODUCTION`, FEFO, lotes, auditoria):
verifica primeiro todas as matérias-primas e **recusa** se faltar alguma (antes punha o stock a zero em silêncio).
O produto acabado entra com o número da ordem como lote. Ver `ProductionOrderLifecycleService` e `ProductionServiceTest`.

## 5. Guia de Transferência (entre armazéns) — o "guia" do negócio

A Guia de Remessa ao cliente é um documento distinto, descrito em §5.1.

A **Guia de Transferência** documenta a movimentação de stock **entre armazéns**:
- Entidade `StockTransfer` + linhas, série `TRF` em `DocumentSeries`.
- Ciclo `DRAFT → PENDING_APPROVAL → APPROVED / REJECTED / CANCELLED`; o stock só sai da origem e entra no
  destino **na aprovação** (FEFO por lote), com permissão MANAGER/ADMIN.
- Lógica em [StockTransferService](src/main/java/mz/multicore/erp/modules/inventory/service/StockTransferService.java),
  PDF em [StockTransferPrintService](src/main/java/mz/multicore/erp/modules/printing/StockTransferPrintService.java).
- Testada por `StockTransferServiceTest` (9 cenários: estados, stock só na aprovação, permissão).

### 5.1 Guia de Remessa ao cliente — expedição a partir da encomenda

Documenta a **mercadoria expedida a um cliente** a partir de uma encomenda.
- Entidade `DeliveryGuide` + linhas, série `GR` em `DocumentSeries` (número único **por empresa**).
- Ciclo `PENDING_APPROVAL → APPROVED / REJECTED / CANCELLED`; o stock **sai (SALE) só na aprovação**
  (FEFO, via `inventoryService.registerMovement` — o mesmo caminho da faturação), MANAGER/ADMIN.
- **Faturação:** gerar a guia tira a encomenda de `PENDING` (→ `GUIDE_PENDING` → `GUIDED`), logo
  `billOrder` deixa de a aceitar. A guia aprovada fatura-se com `DeliveryGuideBillingService`, sem
  mover stock. Anular essa fatura não repõe stock e devolve a encomenda a `GUIDED`.
- Lógica em [DeliveryGuideService](src/main/java/mz/multicore/erp/modules/comercial/service/DeliveryGuideService.java),
  PDF em [DeliveryGuidePrintService](src/main/java/mz/multicore/erp/modules/printing/DeliveryGuidePrintService.java),
  migração `V34`. Testada por `DeliveryGuideServiceTest` (9). Spec/harness:
  [docs/GUIA_REMESSA_ENCOMENDA_SPEC.md](docs/GUIA_REMESSA_ENCOMENDA_SPEC.md).

### 5.2 Cotação — a proposta que antecede a encomenda

Documenta o **preço proposto a um cliente** e por quanto tempo é garantido.
- Entidade `Quotation` + linhas, série `CT` (número único **por empresa**, migração `V44`).
- **Não move nada** — sem stock, dívida, caixa ou contabilidade. O compromisso nasce na conversão.
- **O preço cotado é o preço honrado:** `convert()` copia as linhas da cotação verbatim, tal como
  `billOrder` copia as da encomenda. Não reapreça pelo catálogo.
- **Caducada não converte.** A regra vive no domínio (`Quotation.isExpired`); a caducidade é
  derivada da data, nunca gravada. Estender a validade é acto de **MANAGER/ADMIN**, auditado com a
  validade antiga e a nova.
- **Converte uma só vez** e **só em encomenda** — a fatura continua a sair pelo caminho existente
  (`billOrder`), o único que sabe baixar stock, travar crédito e numerar `FT`.
- As duas portas que criam encomendas (catálogo e cotação) passam por
  `ComercialService.placeOrder`, o único sítio que numera `EC` e submete a aprovação.
- Lógica em [QuotationService](src/main/java/mz/multicore/erp/modules/comercial/service/QuotationService.java),
  PDF em [QuotationPrintService](src/main/java/mz/multicore/erp/modules/printing/QuotationPrintService.java).
  Testada por `QuotationTest` (11) + `QuotationServiceTest` (28). Spec/harness:
  [docs/COTACAO_SPEC.md](docs/COTACAO_SPEC.md) / [docs/COTACAO_HARNESS.md](docs/COTACAO_HARNESS.md).

---

## 6. Onde mexer (mapa rápido de ficheiros)

| Quero…                                  | Ficheiro                                                                                  |
|-----------------------------------------|-------------------------------------------------------------------------------------------|
| Lógica de venda POS                     | [POSService](src/main/java/mz/multicore/erp/modules/pos/service/POSService.java)                |
| Faturação / encomenda / anulação        | [ComercialService](src/main/java/mz/multicore/erp/modules/comercial/service/ComercialService.java) |
| Cotação / conversão em encomenda        | [QuotationService](src/main/java/mz/multicore/erp/modules/comercial/service/QuotationService.java) |
| Reposição interna (loja ⇄ armazém)      | [InternalReplenishmentService](src/main/java/mz/multicore/erp/modules/comercial/service/InternalReplenishmentService.java) |
| **Disponibilidade para sair** de um armazém | `ProductBatchService.materialiseLegacyIfNeeded` — lotes são subdivisão do stock, materializada na 1.ª saída |
| **Validade obrigatória** (perecíveis)   | `StockMovementRegistrationService.requireExpiryForPerishableIntake` — ver [docs/PRODUTO_PERECIVEL_VALIDADE_SPEC.md](docs/PRODUTO_PERECIVEL_VALIDADE_SPEC.md) |
| **Criar uma encomenda** (porta única)   | `ComercialService.placeOrder` — numera `EC` e submete a aprovação |
| Baixa de stock da fatura manual         | [InvoiceApprovalCallback](src/main/java/mz/multicore/erp/modules/comercial/service/InvoiceApprovalCallback.java) |
| Nota de crédito / devolução de stock    | [CreditNoteService](src/main/java/mz/multicore/erp/modules/comercial/service/CreditNoteService.java) |
| Nota de débito                          | [DebitNoteService](src/main/java/mz/multicore/erp/modules/comercial/service/DebitNoteService.java) |
| Tipos de séries de documentos           | [DocumentSeries](src/main/java/mz/multicore/erp/modules/numbering/service/DocumentSeries.java)  |
| Ledger de stock                         | [StockMovementType](src/main/java/mz/multicore/erp/modules/inventory/model/StockMovementType.java) |
| Ledger de caixa                         | [TillMovementType](src/main/java/mz/multicore/erp/modules/pos/model/TillMovementType.java)      |
| Ledger de tesouraria                    | [TransactionType](src/main/java/mz/multicore/erp/modules/financeira/model/TransactionType.java) |

---

## 7. Pontos abertos / dívida técnica

1. ~~**Guia de Remessa ao cliente**~~ — **implementado (2026-07-23)**, revertendo a decisão de
   2026-06-21. `DeliveryGuide` (série `GR`), gerada da encomenda, stock SALE só na aprovação;
   `DeliveryGuideServiceTest` (9). Spec/harness em
   [docs/GUIA_REMESSA_ENCOMENDA_SPEC.md](docs/GUIA_REMESSA_ENCOMENDA_SPEC.md). UI Swing e cliente
   HTTP no desktop: `DeliveryGuidesPanel` e `CommercialDeliveryGuideApiClient`.
2. ~~**Nota de Débito** numera fora de `DocumentSeries`~~ — **resolvido (2026-06-20)**: passou a
   usar `DocumentNumberService.next(DocumentSeries.DEBIT_NOTE)`, série `ND` sequencial e gapless,
   coberto por `DocumentNumberServiceTest`.
3. ~~**Sem visão unificada de "movimentos"**~~ — **resolvido (2026-06-25)**: novo módulo de leitura
   agregada `modules/movimentos/` lista fatura, encomenda, NC e ND num só sítio, filtrável por
   nº/cliente e período, ordenado por data desc. Endpoint `GET /api/movimentos` + tab "Movimentos"
   no `ComercialPanel`. Spec/harness em [docs/MOVIMENTOS_UNIFICADOS_SPEC.md](docs/MOVIMENTOS_UNIFICADOS_SPEC.md)
   / [docs/MOVIMENTOS_UNIFICADOS_HARNESS.md](docs/MOVIMENTOS_UNIFICADOS_HARNESS.md). Testado por
   `MovimentosServiceTest` (7: MU-01..MU-07).
4. ~~**Sem contabilidade**~~ — **resolvido (2026-08-15)**: módulo `accounting` com plano PGC-NIRF,
   diário de partidas dobradas, razão e balancete. Venda e recebimento lançam automaticamente por
   evento. Ver [docs/CONTABILIDADE_SPEC.md](docs/CONTABILIDADE_SPEC.md).
5. ~~**Compras e salários não lançam na contabilidade**~~ — **resolvido**: `ProcurementPostingHandler`
   e `OperationsPostingHandler` lançam compras, pagamentos a fornecedores e salários por evento.
6. ~~**Notas de crédito/débito não geram estorno contabilístico**~~ — **resolvido**:
   `CommercialPostingHandler` lança as fontes `CREDIT_NOTE` e `DEBIT_NOTE` na aprovação.
7. ~~**Quebras e acertos não chegam à contabilidade**~~ — **resolvido (2026-11-10)**, ver §3.
8. ~~**Ordem de produção altera o stock por fora do serviço central**~~ — **resolvido (2026-11-10)**, ver §4.1.
9. **Em aberto (contabilista):** a ordem de produção lança o produto acabado em **3401 Produtos acabados**, mas a
   venda desse produto credita **3201 Mercadorias**. Decidir se os produtos fabricados vão para 3201 (mais simples)
   ou se a venda passa a creditar 3401 para esses artigos.
10. **Em aberto (operação):** recibos recebidos ao balcão (sinal, vasilhame, faturas em dívida) entram na conta de
    tesouraria escolhida e não na gaveta da sessão POS. Formação na loja piloto: escolher a conta "Caixa" certa.

## 8. Documentos e movimentos sectoriais (2026-11)

Nenhum cria documento fiscal paralelo: o fiscal é sempre FT, RC, NC, ND ou GR. Séries internas (não fiscais):

| Documento | Série | Módulo | Movimentos |
|---|---|---|---|
| Talão de levantamento | `LV` | `pickup` | **Nenhum** de stock (a fatura já baixou). Saldo por linha; NC só credita o que não foi levantado (`CreditNoteLineGuard`) |
| Desmancha | `DM` | `butchery` | `PRODUCTION`: carcaça sai, cortes entram com custo repartido; lançamento §3 |
| Lote produzido | `PD…` (lote) | `productionplan` | `PRODUCTION`: ingredientes saem (FEFO), produto entra com validade; lançamento §3 |
| Guia de consignação | `CG` | `consignment` | `TRANSFER` loja → "Em consignação"; no acerto: devolvido volta, estragado `WASTE`, vendido faturado (FT) |
| Talão de pesagem | `TP` | `weighbridge` | Nenhum (só peso); peso manual assinalado |
| Adiantamento | — | `advances` | FT do sinal + RC; aplicar: NC + recibo de compensação (sem caixa nem diário); devolver: NC + anulação do RC |
| Vasilhame | — | `returnables` | `TRANSFER` loja ⇄ "Vasilhame em clientes"; não devolvido faturado (FT) |

Armazéns de trânsito (`TransitWarehouseProvider`, tipo `TRANSIT`, sem venda no POS): **"Em consignação"** e
**"Vasilhame em clientes"**. Custo médio ponderado dos produtos feitos em casa: `AverageCostUpdater`.
Specs: `docs/SECTORES_ACTIVIDADE_SPEC.md` e as specs de cada módulo.

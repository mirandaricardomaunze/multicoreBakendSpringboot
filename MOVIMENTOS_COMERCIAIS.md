# Movimentos Comerciais — Mapa Canónico

> Fonte de verdade sobre **que documentos de venda existem**, **que movimentos cada um gera**
> e **o que ainda falta**. Lê este ficheiro antes de mexer em faturação, POS, notas ou stock.
> Detalhe de camadas em [ARCHITECTURE.md](ARCHITECTURE.md); convenções em [CONVENTIONS.md](CONVENTIONS.md).

**Última actualização:** 2026-06-21

---

## 1. Resposta directa à pergunta

| Movimento pedido        | Existe? | Como está modelado                                                                 |
|-------------------------|:-------:|------------------------------------------------------------------------------------|
| **Venda POS**           | ✅      | Não é documento próprio — é uma `Invoice` com `SalesChannel.POS`, série **FT**.    |
| **Fatura**              | ✅      | `Invoice` (canais `MANUAL`, `POS`, `ORDER`), série **FT**.                          |
| **Nota de Crédito**     | ✅      | `CreditNote`, série **NC**. Devolve stock na aprovação se motivo = `RETURN`.        |
| **Guia (transferência entre armazéns)** | ✅ | `StockTransfer`, série **TRF**. Create/approve/reject/cancel com stock a sair só na aprovação; PDF via `StockTransferPrintService`. |

> ✅ **Decisão (2026-06-21):** o "guia" que o negócio usa é a **Guia de Transferência** entre
> armazéns — e essa já existe e está testada (`StockTransferServiceTest`, 9 testes). A **Guia de
> Remessa/Entrega ao cliente NÃO é requisito** e não será implementada. Os movimentos de venda
> (POS, fatura, NC) estão completos.

---

## 2. Documentos comerciais existentes

Todos vivem em `modules/comercial/` (excepto o ciclo de caixa, em `modules/pos/`) e numeram
pela série central [`DocumentSeries`](src/main/java/com/phcpro/modules/numbering/service/DocumentSeries.java).

| Documento        | Entidade      | Série  | Tabela            | Estado / ciclo                                              |
|------------------|---------------|--------|-------------------|------------------------------------------------------------|
| Encomenda        | `Order`       | `EC`   | `customer_orders` | `PENDING → BILLED / CANCELLED`                             |
| Fatura           | `Invoice`     | `FT`   | `invoices`        | `DRAFT → PENDING_APPROVAL → APPROVED → PAID` / `CANCELLED` |
| Recibo           | `Receipt`     | `RC`   | —                 | `COMPLETED` / anulado                                      |
| Nota de Crédito  | `CreditNote`  | `NC`   | —                 | `PENDING_APPROVAL → APPROVED` / `REJECTED` / `CANCELLED`   |
| Nota de Débito   | `DebitNote`   | `ND`   | —                 | Puramente financeira (sem stock). Numeração sequencial gapless via `DocumentSeries.DEBIT_NOTE`. |

---

## 3. Os três livros de movimentos (não confundir)

O sistema **não tem um livro único de "movimentos comerciais"**. Tem três ledgers
independentes, cada um na sua fronteira. Um mesmo documento pode tocar vários.

| Ledger                | Entidade / Enum                              | Módulo        | O que regista                                  |
|-----------------------|----------------------------------------------|---------------|------------------------------------------------|
| **Stock**             | `StockMovement` / `StockMovementType`        | `inventory`   | `PURCHASE, ENTRY, SALE, TRANSFER, ADJUSTMENT, RETURN, REVERSAL` |
| **Caixa (gaveta)**    | `TillMovement` / `TillMovementType`          | `pos`         | `SALE, SUPRIMENTO, SANGRIA`                    |
| **Tesouraria**        | `TreasuryTransaction` / `TransactionType`    | `financeira`  | `DEBIT` (entrada) / `CREDIT` (saída)           |

Princípio em vigor (ver [POSService](src/main/java/com/phcpro/modules/pos/service/POSService.java)):
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

FATURA DE ENCOMENDA  (Invoice, SalesChannel.ORDER)  ComercialService.billOrder()
  └─ StockMovement SALE (saída, por linha)

ANULAÇÃO DE FATURA   ComercialService.cancelInvoice()
  └─ StockMovement REVERSAL (reposição de stock)

NOTA DE CRÉDITO (motivo RETURN)   CreditNoteService.approve()
  └─ StockMovement RETURN (entrada, reposição no armazém)

NOTA DE CRÉDITO (outros motivos) / NOTA DE DÉBITO
  └─ Sem movimento de stock — só efeito documental/financeiro
```

**Nota importante sobre o timing do stock:** a fatura **POS**, a **de encomenda** e a **manual
sem desconto >10%** baixam stock **no acto** (`createInvoice` chama `registerMovement` via
`deductStockForInvoice`). Só a fatura manual **com desconto >10%** adia a baixa para a **aprovação**
(em [InvoiceApprovalCallback](src/main/java/com/phcpro/modules/comercial/service/InvoiceApprovalCallback.java)).
Decisão de 2026-06-20: emitir fatura é operação directa de quem tem perfil autorizado (atribuído pelo
admin), não passa pela Engine de Aprovações — só o desconto sensível continua a exigir gerente.

---

## 5. Guia de Transferência (entre armazéns) — o "guia" do negócio

**Decisão (2026-06-21): é esta a guia que o negócio usa, e já existe.** A Guia de Remessa/Entrega
ao cliente **não é requisito** e foi descartada.

A **Guia de Transferência** documenta a movimentação de stock **entre armazéns**:
- Entidade `StockTransfer` + linhas, série `TRF` em `DocumentSeries`.
- Ciclo `PENDING_APPROVAL → APPROVED / REJECTED / CANCELLED`; o stock só sai da origem e entra no
  destino **na aprovação** (FEFO por lote), com permissão MANAGER/ADMIN.
- Lógica em [StockTransferService](src/main/java/com/phcpro/modules/inventory/service/StockTransferService.java),
  PDF em [StockTransferPrintService](src/main/java/com/phcpro/modules/printing/StockTransferPrintService.java).
- Testada por `StockTransferServiceTest` (9 cenários: estados, stock só na aprovação, permissão).

---

## 6. Onde mexer (mapa rápido de ficheiros)

| Quero…                                  | Ficheiro                                                                                  |
|-----------------------------------------|-------------------------------------------------------------------------------------------|
| Lógica de venda POS                     | [POSService](src/main/java/com/phcpro/modules/pos/service/POSService.java)                |
| Faturação / encomenda / anulação        | [ComercialService](src/main/java/com/phcpro/modules/comercial/service/ComercialService.java) |
| Baixa de stock da fatura manual         | [InvoiceApprovalCallback](src/main/java/com/phcpro/modules/comercial/service/InvoiceApprovalCallback.java) |
| Nota de crédito / devolução de stock    | [CreditNoteService](src/main/java/com/phcpro/modules/comercial/service/CreditNoteService.java) |
| Nota de débito                          | [DebitNoteService](src/main/java/com/phcpro/modules/comercial/service/DebitNoteService.java) |
| Tipos de séries de documentos           | [DocumentSeries](src/main/java/com/phcpro/modules/numbering/service/DocumentSeries.java)  |
| Ledger de stock                         | [StockMovementType](src/main/java/com/phcpro/modules/inventory/model/StockMovementType.java) |
| Ledger de caixa                         | [TillMovementType](src/main/java/com/phcpro/modules/pos/model/TillMovementType.java)      |
| Ledger de tesouraria                    | [TransactionType](src/main/java/com/phcpro/modules/financeira/model/TransactionType.java) |

---

## 7. Pontos abertos / dívida técnica

1. ~~**Guia de Remessa ao cliente**~~ — **decidido (2026-06-21): não é requisito.** O "guia" do
   negócio é a Guia de Transferência entre armazéns, que já existe e está testada (§5).
2. ~~**Nota de Débito** numera fora de `DocumentSeries`~~ — **resolvido (2026-06-20)**: passou a
   usar `DocumentNumberService.next(DocumentSeries.DEBIT_NOTE)`, série `ND` sequencial e gapless,
   coberto por `DocumentNumberServiceTest`.
3. **Sem visão unificada de "movimentos"** — não há ecrã/relatório que liste todos os documentos
   de um cliente/período num só sítio; cada tipo consulta-se no seu painel. Avaliar um
   `MovimentosController` de leitura agregada se o negócio o pedir.

# Movimentos Comerciais — Mapa Canónico

> Fonte de verdade sobre **que documentos de venda existem**, **que movimentos cada um gera**
> e **o que ainda falta**. Lê este ficheiro antes de mexer em faturação, POS, notas ou stock.
> Detalhe de camadas em [ARCHITECTURE.md](ARCHITECTURE.md); convenções em [CONVENTIONS.md](CONVENTIONS.md).

**Última actualização:** 2026-06-16

---

## 1. Resposta directa à pergunta

| Movimento pedido        | Existe? | Como está modelado                                                                 |
|-------------------------|:-------:|------------------------------------------------------------------------------------|
| **Venda POS**           | ✅      | Não é documento próprio — é uma `Invoice` com `SalesChannel.POS`, série **FT**.    |
| **Fatura**              | ✅      | `Invoice` (canais `MANUAL`, `POS`, `ORDER`), série **FT**.                          |
| **Nota de Crédito**     | ✅      | `CreditNote`, série **NC**. Devolve stock na aprovação se motivo = `RETURN`.        |
| **Guia (de remessa/entrega ao cliente)** | ❌ **NÃO existe** | Ver §5. O único "Guia" é a **Guia de Transferência** de stock (interna, não-fiscal). |

> ⚠️ **Conclusão principal:** os movimentos de venda (POS, fatura, NC) estão completos, mas
> **não há documento de Guia de Remessa/Entrega ao cliente**. A "Guia de Transferência"
> existente é logística interna entre armazéns — não documenta entrega a cliente nem é
> movimento comercial. Se o requisito legal/operacional exige guia de remessa, é trabalho novo.

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
| Nota de Débito   | `DebitNote`   | `ND-…` | —                 | Puramente financeira (sem stock). ⚠️ numeração própria, **fora** de `DocumentSeries` |

> ⚠️ **Dívida técnica de numeração:** a Nota de Débito gera o número à mão
> (`"ND-" + timestamp` em `DebitNoteService`), não usa `DocumentNumberService`. Inconsistente
> com as restantes séries (sem sequencial sem saltos por ano). Alinhar quando se tocar no módulo.

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
  └─ Passa pela Engine de Aprovações → ao APROVAR:  InvoiceApprovalCallback.onApproved()
        └─ StockMovement SALE (saída, por linha)
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

**Nota importante sobre o timing do stock:** a fatura **POS** e a **de encomenda** baixam stock
no acto; a fatura **manual** só baixa stock **na aprovação** (callback). Não procures o
`registerMovement` dentro de `createInvoice` — ele está em
[InvoiceApprovalCallback](src/main/java/com/phcpro/modules/comercial/service/InvoiceApprovalCallback.java).

---

## 5. A lacuna: Guia de Remessa / Entrega ao cliente

**Não existe.** Procuras de "guia" no código só encontram a **Guia de Transferência**
([StockTransferPrintService](src/main/java/com/phcpro/modules/printing/StockTransferPrintService.java),
série `TRF`), que é movimento de stock **entre armazéns** — não há cliente, não há valor fiscal,
não é entrega.

Se for preciso suportar guia de remessa (expedição de mercadoria sem/antes de faturar, com
faturação posterior), o desenho que respeita a arquitectura actual seria:

- Nova entidade `DeliveryNote` em `modules/comercial/model/` (extends `BaseEntity`), com linhas.
- Série nova em `DocumentSeries` (ex.: `GR`).
- Gera `StockMovement SALE` (saída) na expedição; a fatura posterior **não** repete a baixa.
- Liga-se a `Order`/`Invoice` por referência, à semelhança de `CreditNote → Invoice`.
- Controller → Service → Repository separados (regra do projecto).
- Usar a skill `phc-new-module` ou `phc-new-endpoint` para o scaffold.

> Decisão a tomar com o utilizador **antes** de implementar: precisa mesmo de guia de remessa,
> ou o fluxo Encomenda → Fatura já cobre o caso de uso? (ver regra 7 do [CLAUDE.md](CLAUDE.md)).

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

1. **Guia de Remessa ao cliente não existe** — decidir se é requisito (§5).
2. **Nota de Débito** numera fora de `DocumentSeries` (`ND-` + timestamp) — alinhar com as séries sequenciais.
3. **Sem visão unificada de "movimentos"** — não há ecrã/relatório que liste todos os documentos
   de um cliente/período num só sítio; cada tipo consulta-se no seu painel. Avaliar um
   `MovimentosController` de leitura agregada se o negócio o pedir.

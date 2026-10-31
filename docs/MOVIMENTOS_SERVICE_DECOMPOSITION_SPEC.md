# Especificação Técnica de Decomposição - MovimentosService (Fase 113)

## 1. Contexto & Diagnóstico
O serviço `MovimentosService.java` (416 linhas) no módulo `backend` agregava em uma única classe:
- Injeção e manuseio direto de 8 repositórios comerciais (`InvoiceRepository`, `OrderRepository`, `CreditNoteRepository`, `DebitNoteRepository`, `DeliveryGuideRepository`, `QuotationRepository`, `ReceiptRepository`, `StockTransferRepository`).
- Transformação de entidades de 8 domínios para `MovimentoDTO`.
- Métodos de sumarização textual de linhas para cada um dos 8 tipos de documento (`summarizeInvoice`, `summarizeQuotation`, `summarizeOrder`, `summarizeGuide`, `summarizeCreditNote`, `summarizeDebitNote`, `summarizeTransfer`).
- Métodos de agregação matemática/quantitativa (`sumInvoiceQty`, `sumQuotationQty`, `sumOrderQty`, `sumGuideQty`, `sumCreditNoteQty`, `sumTransferQty`).
- Regras de formatação de nome de cliente e walk-in (`nameOf`).
- Lógica de filtragem em memória (`matchesQuery`, `withinPeriod`) e ordenação (`byDateDescNullsLast`).

## 2. Metas de Decomposição e Limites Físicos
- Orquestrador:
  - `MovimentosService.java`: $\le 100$ linhas (meta atual: ~75 linhas, redução $\ge 75\%$).
- Colaboradores:
  - `MovimentoSummarizer.java`: $\le 190$ linhas (responsabilidade única: formatação de nomes, sumarização de linhas de artigos e soma de quantidades).
  - `MovimentoCollector.java`: $\le 205$ linhas (responsabilidade única: coleta de documentos dos repositórios por `companyId`, montagem dos DTOs e filtragem/ordenação).

## 3. Preservação de Contratos & Retrocompatibilidade
- Preserva `@Service` em `MovimentosService`.
- Preserva construtor canônico de 6 repositórios: `MovimentosService(InvoiceRepository, OrderRepository, CreditNoteRepository, DebitNoteRepository, DeliveryGuideRepository, QuotationRepository)`.
- Preserva construtor canônico de 8 repositórios: `MovimentosService(InvoiceRepository, OrderRepository, CreditNoteRepository, DebitNoteRepository, DeliveryGuideRepository, QuotationRepository, ReceiptRepository, StockTransferRepository)`.
- Suporta injeção de `MovimentoCollector` via construtor principal `@Autowired`.
- Todas as validações multi-tenant (`CurrentUserContext.requireCompany(companyId)`) mantidas estritamente no início de `listar(...)`.
- Zero uso de `BigDecimal.ZERO.equals(...)`.
- Zero dependência Swing ou `JOptionPane` no backend.

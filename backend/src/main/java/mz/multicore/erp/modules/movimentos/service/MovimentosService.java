package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoTipo;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Leitura agregada dos documentos comerciais (fatura, encomenda, nota de crédito, nota de
 * débito) numa só lista filtrável por cliente/nº e período. Read-only: não cria nem altera
 * documentos. Reutiliza os repositórios de {@code comercial} (mesmo padrão de {@code ReportService}).
 */
@Service
public class MovimentosService {

    private static final String SEM_CLIENTE = "—";

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final DeliveryGuideRepository deliveryGuideRepository;
    private final QuotationRepository quotationRepository;
    private final ReceiptRepository receiptRepository;
    private final StockTransferRepository stockTransferRepository;

    public MovimentosService(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository,
            DeliveryGuideRepository deliveryGuideRepository,
            QuotationRepository quotationRepository
    ) {
        this(invoiceRepository, orderRepository, creditNoteRepository, debitNoteRepository,
                deliveryGuideRepository, quotationRepository, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MovimentosService(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository,
            DeliveryGuideRepository deliveryGuideRepository,
            QuotationRepository quotationRepository,
            ReceiptRepository receiptRepository,
            StockTransferRepository stockTransferRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.deliveryGuideRepository = deliveryGuideRepository;
        this.quotationRepository = quotationRepository;
        this.receiptRepository = receiptRepository;
        this.stockTransferRepository = stockTransferRepository;
    }

    /**
     * Lista os movimentos comerciais da empresa, ordenados por data descendente.
     *
     * @param companyId empresa activa (validada contra o contexto do utilizador)
     * @param query     substring case-insensitive sobre nº OU cliente; nulo/vazio = sem filtro
     * @param from      data mínima inclusiva; nulo = sem limite inferior
     * @param to        data máxima inclusiva; nulo = sem limite superior
     */
    @Transactional(readOnly = true)
    public List<MovimentoDTO> listar(Long companyId, String query, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);

        List<MovimentoDTO> movimentos = new ArrayList<>();

        for (Invoice inv : invoiceRepository.findByCompanyId(companyId)) {
            MovimentoTipo tipo = (inv.getSalesChannel() == SalesChannel.POS)
                    ? MovimentoTipo.VENDA_POS
                    : MovimentoTipo.FATURA;
            String wh = inv.getWarehouse() != null ? inv.getWarehouse().getName() : "—";
            movimentos.add(new MovimentoDTO(
                    tipo,
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    nameOf(inv.getClient(), inv.getCustomerName()),
                    inv.getCreatedAt(),
                    statusName(inv.getStatus()),
                    inv.getTotalAmount(),
                    summarizeInvoice(inv),
                    sumInvoiceQty(inv),
                    wh));
        }
        // A cotação é movimento comercial mesmo sem mover stock: quem pergunta "o que é que
        // prometemos a este cliente?" procura no mesmo sítio onde procura tudo o resto.
        for (Quotation quotation : quotationRepository.findByCompanyIdOrderByQuotationDateDesc(companyId)) {
            String wh = quotation.getWarehouse() != null ? quotation.getWarehouse().getName() : "—";
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.COTACAO,
                    quotation.getId(),
                    quotation.getQuotationNumber(),
                    nameOf(quotation.getClient(), quotation.getWalkInName()),
                    quotation.getQuotationDate(),
                    statusName(quotation.getStatus()),
                    quotation.getTotalAmount(),
                    summarizeQuotation(quotation),
                    sumQuotationQty(quotation),
                    wh));
        }
        for (Order order : orderRepository.findByCompanyId(companyId)) {
            String wh = order.getWarehouse() != null ? order.getWarehouse().getName() : "—";
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.ENCOMENDA,
                    order.getId(),
                    order.getOrderNumber(),
                    nameOf(order.getClient(), order.getWalkInName()),
                    order.getCreatedAt(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    summarizeOrder(order),
                    sumOrderQty(order),
                    wh));
        }
        for (DeliveryGuide guide : deliveryGuideRepository.findByCompanyId(companyId)) {
            String wh = guide.getWarehouse() != null ? guide.getWarehouse().getName() : "—";
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.GUIA_REMESSA,
                    guide.getId(),
                    guide.getGuideNumber(),
                    nameOf(guide.getClient(), guide.getWalkInName()),
                    guide.getGuideDate(),
                    statusName(guide.getStatus()),
                    guide.getTotalAmount(),
                    summarizeGuide(guide),
                    sumGuideQty(guide),
                    wh));
        }
        for (CreditNote note : creditNoteRepository.findByCompanyId(companyId)) {
            String wh = note.getWarehouse() != null ? note.getWarehouse().getName()
                    : (note.getInvoice() != null && note.getInvoice().getWarehouse() != null ? note.getInvoice().getWarehouse().getName() : "—");
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.NOTA_CREDITO,
                    note.getId(),
                    note.getNoteNumber(),
                    nameOf(note.getClient(), null),
                    note.getIssueDate(),
                    statusName(note.getStatus()),
                    note.getTotalAmount(),
                    summarizeCreditNote(note),
                    sumCreditNoteQty(note),
                    wh));
        }
        for (DebitNote note : debitNoteRepository.findByCompanyId(companyId)) {
            String wh = (note.getInvoice() != null && note.getInvoice().getWarehouse() != null)
                    ? note.getInvoice().getWarehouse().getName() : "—";
            movimentos.add(new MovimentoDTO(
                    MovimentoTipo.NOTA_DEBITO,
                    note.getId(),
                    note.getNoteNumber(),
                    nameOf(note.getClient(), null),
                    note.getIssueDate(),
                    statusName(note.getStatus()),
                    note.getTotalAmount(),
                    summarizeDebitNote(note),
                    BigDecimal.ZERO,
                    wh));
        }

        if (receiptRepository != null) {
            for (Receipt r : receiptRepository.findByCompanyId(companyId)) {
                String clientName = (r.getInvoice() != null && r.getInvoice().getClient() != null)
                        ? nameOf(r.getInvoice().getClient(), r.getInvoice().getCustomerName())
                        : SEM_CLIENTE;
                String wh = (r.getInvoice() != null && r.getInvoice().getWarehouse() != null)
                        ? r.getInvoice().getWarehouse().getName() : "—";
                movimentos.add(new MovimentoDTO(
                    MovimentoTipo.RECIBO,
                    r.getId(),
                    r.getReceiptNumber(),
                    clientName,
                    r.getReceiptDate(),
                    r.getStatus() != null ? r.getStatus() : "COMPLETED",
                    r.getAmountPaid() != null ? r.getAmountPaid() : BigDecimal.ZERO,
                    r.getInvoice() != null ? summarizeInvoice(r.getInvoice()) : "",
                    r.getInvoice() != null ? sumInvoiceQty(r.getInvoice()) : BigDecimal.ZERO,
                    wh));
            }
        }

        if (stockTransferRepository != null) {
            for (StockTransfer t : stockTransferRepository.findByCompanyId(companyId)) {
                String route = (t.getOriginWarehouse() != null ? t.getOriginWarehouse().getName() : "-")
                        + " → " + (t.getDestinationWarehouse() != null ? t.getDestinationWarehouse().getName() : "-");
                movimentos.add(new MovimentoDTO(
                    MovimentoTipo.TRANSFERENCIA,
                    t.getId(),
                    t.getTransferNumber(),
                    route,
                    t.getTransferDate(),
                    statusName(t.getStatus()),
                    BigDecimal.ZERO,
                    summarizeTransfer(t),
                    sumTransferQty(t),
                    route));
            }
        }

        return movimentos.stream()
                .filter(m -> matchesQuery(m, query))
                .filter(m -> withinPeriod(m, from, to))
                .sorted(byDateDescNullsLast())
                .toList();
    }

    private static String summarizeInvoice(Invoice inv) {
        if (inv == null || inv.getLines() == null) return "";
        return inv.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String summarizeQuotation(Quotation q) {
        if (q == null || q.getLines() == null) return "";
        return q.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String summarizeOrder(Order o) {
        if (o == null || o.getLines() == null) return "";
        return o.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String summarizeGuide(DeliveryGuide g) {
        if (g == null || g.getLines() == null) return "";
        return g.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String summarizeCreditNote(CreditNote n) {
        if (n == null || n.getLines() == null) return "";
        return n.getLines().stream()
                .map(l -> {
                    if (l.getProduct() != null) return l.getProduct().getName();
                    if (l.getInvoiceLine() != null && l.getInvoiceLine().getProduct() != null) {
                        return l.getInvoiceLine().getProduct().getName();
                    }
                    return null;
                })
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String summarizeDebitNote(DebitNote n) {
        if (n == null) return "";
        if (n.getLines() != null && !n.getLines().isEmpty()) {
            return n.getLines().stream()
                    .map(DebitNoteLine::getDescription)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
        }
        return n.getDescription() != null ? n.getDescription() : "";
    }

    private static String summarizeTransfer(StockTransfer t) {
        if (t == null || t.getLines() == null) return "";
        return t.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static BigDecimal sumInvoiceQty(Invoice inv) {
        if (inv == null || inv.getLines() == null) return BigDecimal.ZERO;
        return inv.getLines().stream()
                .map(InvoiceLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumQuotationQty(Quotation q) {
        if (q == null || q.getLines() == null) return BigDecimal.ZERO;
        return q.getLines().stream()
                .map(QuotationLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumOrderQty(Order o) {
        if (o == null || o.getLines() == null) return BigDecimal.ZERO;
        return o.getLines().stream()
                .map(OrderLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumGuideQty(DeliveryGuide g) {
        if (g == null || g.getLines() == null) return BigDecimal.ZERO;
        return g.getLines().stream()
                .map(DeliveryGuideLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumCreditNoteQty(CreditNote n) {
        if (n == null || n.getLines() == null) return BigDecimal.ZERO;
        return n.getLines().stream()
                .map(CreditNoteLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumTransferQty(StockTransfer t) {
        if (t == null || t.getLines() == null) return BigDecimal.ZERO;
        return t.getLines().stream()
                .map(StockTransferLine::getQuantity)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String nameOf(Client client, String fallback) {
        if (client != null && client.getName() != null && !client.getName().isBlank()) {
            return client.getName();
        }
        return fallback != null && !fallback.isBlank() ? fallback : SEM_CLIENTE;
    }

    private static String statusName(Enum<?> status) {
        return status == null ? "" : status.name();
    }

    private static boolean matchesQuery(MovimentoDTO m, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String term = query.trim().toLowerCase();
        String numero = m.numero() == null ? "" : m.numero().toLowerCase();
        String cliente = m.cliente() == null ? "" : m.cliente().toLowerCase();
        String artigos = m.artigos() == null ? "" : m.artigos().toLowerCase();
        String armazem = m.armazem() == null ? "" : m.armazem().toLowerCase();
        return numero.contains(term) || cliente.contains(term) || artigos.contains(term) || armazem.contains(term);
    }

    private static boolean withinPeriod(MovimentoDTO m, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (m.data() == null) {
            return false;
        }
        LocalDate d = m.data().toLocalDate();
        if (from != null && d.isBefore(from)) {
            return false;
        }
        return to == null || !d.isAfter(to);
    }

    private static Comparator<MovimentoDTO> byDateDescNullsLast() {
        return Comparator.comparing(MovimentoDTO::data,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }
}

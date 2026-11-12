package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import mz.multicore.erp.modules.movimentos.dto.MovimentoTipo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static mz.multicore.erp.modules.movimentos.service.MovimentoSummarizer.*;

/**
 * Coletor de movimentos comerciais a partir dos repositórios do sistema.
 */
@Component
public class MovimentoCollector {

    private final InvoiceLookup invoiceLookup;
    private final OrderRepository orderRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final DeliveryGuideRepository deliveryGuideRepository;
    private final QuotationRepository quotationRepository;
    private final ReceiptRepository receiptRepository;
    private final StockTransferRepository stockTransferRepository;

    @Autowired
    public MovimentoCollector(
            InvoiceLookup invoiceLookup,
            OrderRepository orderRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository,
            DeliveryGuideRepository deliveryGuideRepository,
            QuotationRepository quotationRepository,
            @Autowired(required = false) ReceiptRepository receiptRepository,
            @Autowired(required = false) StockTransferRepository stockTransferRepository
    ) {
        this.invoiceLookup = invoiceLookup;
        this.orderRepository = orderRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.deliveryGuideRepository = deliveryGuideRepository;
        this.quotationRepository = quotationRepository;
        this.receiptRepository = receiptRepository;
        this.stockTransferRepository = stockTransferRepository;
    }

    public List<MovimentoDTO> collectMovimentos(Long companyId, String query, LocalDate from, LocalDate to) {
        List<MovimentoDTO> movimentos = new ArrayList<>();
        collectInvoices(companyId, movimentos);
        collectQuotations(companyId, movimentos);
        collectOrders(companyId, movimentos);
        collectDeliveryGuides(companyId, movimentos);
        collectCreditNotes(companyId, movimentos);
        collectDebitNotes(companyId, movimentos);
        collectReceipts(companyId, movimentos);
        collectTransfers(companyId, movimentos);

        return movimentos.stream()
                .filter(m -> matchesQuery(m, query))
                .filter(m -> withinPeriod(m, from, to))
                .sorted(byDateDescNullsLast())
                .toList();
    }

    private void collectInvoices(Long companyId, List<MovimentoDTO> out) {
        if (invoiceLookup == null) return;
        for (Invoice inv : invoiceLookup.listForCompany(companyId)) {
            MovimentoTipo tipo = (inv.getSalesChannel() == SalesChannel.POS) ? MovimentoTipo.VENDA_POS : MovimentoTipo.FATURA;
            String wh = inv.getWarehouse() != null ? inv.getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(tipo, inv.getId(), inv.getInvoiceNumber(), nameOf(inv.getClient(), inv.getCustomerName()),
                    inv.getCreatedAt(), statusName(inv.getStatus()), inv.getTotalAmount(), summarizeInvoice(inv), sumInvoiceQty(inv), wh));
        }
    }

    private void collectQuotations(Long companyId, List<MovimentoDTO> out) {
        if (quotationRepository == null) return;
        for (Quotation q : quotationRepository.findByCompanyIdOrderByQuotationDateDesc(companyId)) {
            String wh = q.getWarehouse() != null ? q.getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(MovimentoTipo.COTACAO, q.getId(), q.getQuotationNumber(), nameOf(q.getClient(), q.getWalkInName()),
                    q.getQuotationDate(), statusName(q.getStatus()), q.getTotalAmount(), summarizeQuotation(q), sumQuotationQty(q), wh));
        }
    }

    private void collectOrders(Long companyId, List<MovimentoDTO> out) {
        if (orderRepository == null) return;
        for (Order o : orderRepository.findByCompanyId(companyId)) {
            String wh = o.getWarehouse() != null ? o.getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(MovimentoTipo.ENCOMENDA, o.getId(), o.getOrderNumber(), nameOf(o.getClient(), o.getWalkInName()),
                    o.getCreatedAt(), o.getStatus(), o.getTotalAmount(), summarizeOrder(o), sumOrderQty(o), wh));
        }
    }

    private void collectDeliveryGuides(Long companyId, List<MovimentoDTO> out) {
        if (deliveryGuideRepository == null) return;
        for (DeliveryGuide g : deliveryGuideRepository.findByCompanyId(companyId)) {
            String wh = g.getWarehouse() != null ? g.getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(MovimentoTipo.GUIA_REMESSA, g.getId(), g.getGuideNumber(), nameOf(g.getClient(), g.getWalkInName()),
                    g.getGuideDate(), statusName(g.getStatus()), g.getTotalAmount(), summarizeGuide(g), sumGuideQty(g), wh));
        }
    }

    private void collectCreditNotes(Long companyId, List<MovimentoDTO> out) {
        if (creditNoteRepository == null) return;
        for (CreditNote n : creditNoteRepository.findByCompanyId(companyId)) {
            String wh = n.getWarehouse() != null ? n.getWarehouse().getName()
                    : (n.getInvoice() != null && n.getInvoice().getWarehouse() != null ? n.getInvoice().getWarehouse().getName() : "—");
            out.add(new MovimentoDTO(MovimentoTipo.NOTA_CREDITO, n.getId(), n.getNoteNumber(), nameOf(n.getClient(), null),
                    n.getIssueDate(), statusName(n.getStatus()), n.getTotalAmount(), summarizeCreditNote(n), sumCreditNoteQty(n), wh));
        }
    }

    private void collectDebitNotes(Long companyId, List<MovimentoDTO> out) {
        if (debitNoteRepository == null) return;
        for (DebitNote n : debitNoteRepository.findByCompanyId(companyId)) {
            String wh = (n.getInvoice() != null && n.getInvoice().getWarehouse() != null) ? n.getInvoice().getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(MovimentoTipo.NOTA_DEBITO, n.getId(), n.getNoteNumber(), nameOf(n.getClient(), null),
                    n.getIssueDate(), statusName(n.getStatus()), n.getTotalAmount(), summarizeDebitNote(n), BigDecimal.ZERO, wh));
        }
    }

    private void collectReceipts(Long companyId, List<MovimentoDTO> out) {
        if (receiptRepository == null) return;
        for (Receipt r : receiptRepository.findByCompanyId(companyId)) {
            String clientName = (r.getInvoice() != null && r.getInvoice().getClient() != null)
                    ? nameOf(r.getInvoice().getClient(), r.getInvoice().getCustomerName()) : SEM_CLIENTE;
            String wh = (r.getInvoice() != null && r.getInvoice().getWarehouse() != null) ? r.getInvoice().getWarehouse().getName() : "—";
            out.add(new MovimentoDTO(MovimentoTipo.RECIBO, r.getId(), r.getReceiptNumber(), clientName, r.getReceiptDate(),
                    r.getStatus() != null ? r.getStatus() : "COMPLETED", r.getAmountPaid() != null ? r.getAmountPaid() : BigDecimal.ZERO,
                    r.getInvoice() != null ? summarizeInvoice(r.getInvoice()) : "",
                    r.getInvoice() != null ? sumInvoiceQty(r.getInvoice()) : BigDecimal.ZERO, wh));
        }
    }

    private void collectTransfers(Long companyId, List<MovimentoDTO> out) {
        if (stockTransferRepository == null) return;
        for (StockTransfer t : stockTransferRepository.findByCompanyId(companyId)) {
            String route = (t.getOriginWarehouse() != null ? t.getOriginWarehouse().getName() : "-")
                    + " → " + (t.getDestinationWarehouse() != null ? t.getDestinationWarehouse().getName() : "-");
            out.add(new MovimentoDTO(MovimentoTipo.TRANSFERENCIA, t.getId(), t.getTransferNumber(), route, t.getTransferDate(),
                    statusName(t.getStatus()), BigDecimal.ZERO, summarizeTransfer(t), sumTransferQty(t), route));
        }
    }

    private static boolean matchesQuery(MovimentoDTO m, String query) {
        if (query == null || query.isBlank()) return true;
        String term = query.trim().toLowerCase();
        String numero = m.numero() == null ? "" : m.numero().toLowerCase();
        String cliente = m.cliente() == null ? "" : m.cliente().toLowerCase();
        String artigos = m.artigos() == null ? "" : m.artigos().toLowerCase();
        String armazem = m.armazem() == null ? "" : m.armazem().toLowerCase();
        return numero.contains(term) || cliente.contains(term) || artigos.contains(term) || armazem.contains(term);
    }

    private static boolean withinPeriod(MovimentoDTO m, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (m.data() == null) return false;
        LocalDate d = m.data().toLocalDate();
        if (from != null && d.isBefore(from)) return false;
        return to == null || !d.isAfter(to);
    }

    private static Comparator<MovimentoDTO> byDateDescNullsLast() {
        return Comparator.comparing(MovimentoDTO::data, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}

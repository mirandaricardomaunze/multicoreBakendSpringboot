package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Funções puras de sumarização de artigos, quantidades e nomes de clientes para movimentos comerciais.
 */
public final class MovimentoSummarizer {

    public static final String SEM_CLIENTE = "—";

    private MovimentoSummarizer() {}

    public static String summarizeInvoice(Invoice inv) {
        if (inv == null || inv.getLines() == null) return "";
        return inv.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static String summarizeQuotation(Quotation q) {
        if (q == null || q.getLines() == null) return "";
        return q.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static String summarizeOrder(Order o) {
        if (o == null || o.getLines() == null) return "";
        return o.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static String summarizeGuide(DeliveryGuide g) {
        if (g == null || g.getLines() == null) return "";
        return g.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static String summarizeCreditNote(CreditNote n) {
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
                .collect(Collectors.joining(", "));
    }

    public static String summarizeDebitNote(DebitNote n) {
        if (n == null) return "";
        if (n.getLines() != null && !n.getLines().isEmpty()) {
            return n.getLines().stream()
                    .map(DebitNoteLine::getDescription)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
        return n.getDescription() != null ? n.getDescription() : "";
    }

    public static String summarizeTransfer(StockTransfer t) {
        if (t == null || t.getLines() == null) return "";
        return t.getLines().stream()
                .map(l -> l.getProduct() != null ? l.getProduct().getName() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static BigDecimal sumInvoiceQty(Invoice inv) {
        if (inv == null || inv.getLines() == null) return BigDecimal.ZERO;
        return inv.getLines().stream()
                .map(InvoiceLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumQuotationQty(Quotation q) {
        if (q == null || q.getLines() == null) return BigDecimal.ZERO;
        return q.getLines().stream()
                .map(QuotationLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumOrderQty(Order o) {
        if (o == null || o.getLines() == null) return BigDecimal.ZERO;
        return o.getLines().stream()
                .map(OrderLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumGuideQty(DeliveryGuide g) {
        if (g == null || g.getLines() == null) return BigDecimal.ZERO;
        return g.getLines().stream()
                .map(DeliveryGuideLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumCreditNoteQty(CreditNote n) {
        if (n == null || n.getLines() == null) return BigDecimal.ZERO;
        return n.getLines().stream()
                .map(CreditNoteLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal sumTransferQty(StockTransfer t) {
        if (t == null || t.getLines() == null) return BigDecimal.ZERO;
        return t.getLines().stream()
                .map(StockTransferLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static String nameOf(Client client, String fallback) {
        boolean hasFallback = fallback != null && !fallback.isBlank();
        boolean isWalkIn = client == null
                || WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())
                || WalkInClientProvider.WALK_IN_NAME.equalsIgnoreCase(client.getName());

        if (isWalkIn) {
            return hasFallback ? fallback.trim() : WalkInClientProvider.WALK_IN_NAME;
        }
        if (client != null && client.getName() != null && !client.getName().isBlank()) {
            return hasFallback ? client.getName() + " — " + fallback.trim() : client.getName();
        }
        return hasFallback ? fallback.trim() : SEM_CLIENTE;
    }

    public static String statusName(Enum<?> status) {
        return status == null ? "" : status.name();
    }
}

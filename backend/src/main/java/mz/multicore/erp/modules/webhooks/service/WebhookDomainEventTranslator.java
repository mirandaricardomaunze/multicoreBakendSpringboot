package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.architecture.events.CreditNoteApprovedEvent;
import mz.multicore.erp.architecture.events.OrderStatusChangedEvent;
import mz.multicore.erp.architecture.events.SaleRegisteredEvent;
import mz.multicore.erp.architecture.events.StockAvailabilityChangedEvent;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceNoteLookup;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.CreditNoteIssued;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.InvoiceIssued;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.OrderStatusChanged;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.StockAvailabilityChanged;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Turns a domain event into the {@code data} summary of its webhook (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1,
 * WH-02). What the domain event lacks (customer, NUIT, channel, invoice number) is read through the owning module's
 * public lookup, never its Repository. Returns empty when the fact does not deserve a webhook: unknown document,
 * order status that did not change, or stock that did not cross zero.
 */
@Component
public class WebhookDomainEventTranslator {

    private final InvoiceLookup invoiceLookup;
    private final InvoiceNoteLookup noteLookup;
    private final OrderLookup orderLookup;

    public WebhookDomainEventTranslator(InvoiceLookup invoiceLookup, InvoiceNoteLookup noteLookup,
                                        OrderLookup orderLookup) {
        this.invoiceLookup = invoiceLookup;
        this.noteLookup = noteLookup;
        this.orderLookup = orderLookup;
    }

    /** Fact of an {@code invoice.issued}: the invoice, invoice-receipt or POS sale. */
    Optional<TranslatedWebhookEvent> invoiceIssued(SaleRegisteredEvent event) {
        return invoiceLookup.find(event.invoiceId()).map(invoice -> new TranslatedWebhookEvent(
                instant(invoice.getCreatedAt()),
                new InvoiceIssued(invoice.getInvoiceNumber(),
                        invoice.getDocumentDate() != null ? invoice.getDocumentDate() : event.date(),
                        invoice.getSalesChannel() == SalesChannel.POS ? "POS" : "MANUAL",
                        customer(invoice.getCustomerName(), invoice.getClient()),
                        event.netAmount(), event.taxAmount(), event.totalAmount(), event.amountPaidNow())));
    }

    /** Fact of a {@code credit_note.issued}: the approved credit note. */
    Optional<TranslatedWebhookEvent> creditNoteIssued(CreditNoteApprovedEvent event) {
        return noteLookup.findCreditNote(event.noteId()).map(note -> {
            Invoice invoice = note.getInvoice();
            Client client = note.getClient() != null ? note.getClient() : invoice == null ? null : invoice.getClient();
            return new TranslatedWebhookEvent(instant(approvedAt(note)),
                    new CreditNoteIssued(note.getNoteNumber(), invoice == null ? null : invoice.getInvoiceNumber(),
                            customer(invoice == null ? null : invoice.getCustomerName(), client),
                            event.netAmount(), event.taxAmount(), event.totalAmount()));
        });
    }

    /** Fact of an {@code order.status_changed}; nothing when the status did not change. */
    Optional<TranslatedWebhookEvent> orderStatusChanged(OrderStatusChangedEvent event) {
        if (event.unchanged()) {
            return Optional.empty();
        }
        return orderLookup.findOrderForCompany(event.orderId(), event.companyId()).map(order ->
                new TranslatedWebhookEvent(event.occurredAt(),
                        new OrderStatusChanged(order.getOrderNumber(), event.previousStatus(), event.status(),
                                customer(order.getWalkInName(), order.getClient()), order.getTotalAmount(),
                                invoiceNumber(order))));
    }

    /** Fact of a {@code stock.availability_changed}; nothing when the available stock did not cross zero. */
    Optional<TranslatedWebhookEvent> stockAvailabilityChanged(StockAvailabilityChangedEvent event) {
        String status = event.wentOutOfStock() ? StockAvailabilityChanged.OUT_OF_STOCK
                : event.cameBackInStock() ? StockAvailabilityChanged.BACK_IN_STOCK : null;
        if (status == null) {
            return Optional.empty();
        }
        return Optional.of(new TranslatedWebhookEvent(event.occurredAt(),
                new StockAvailabilityChanged(event.sku(), event.productName(), event.warehouseCode(),
                        event.warehouseName(), event.availableAfter(), status)));
    }

    private String invoiceNumber(Order order) {
        return invoiceLookup.find(order.getInvoiceId()).map(Invoice::getInvoiceNumber).orElse(null);
    }

    private static WebhookCustomer customer(String documentName, Client client) {
        return WebhookCustomer.of(documentName, client == null ? null : client.getName(),
                client == null ? null : client.getTaxId());
    }

    private static LocalDateTime approvedAt(CreditNote note) {
        return note.getApprovedAt() != null ? note.getApprovedAt() : note.getCreatedAt();
    }

    private static OffsetDateTime instant(LocalDateTime local) {
        LocalDateTime value = local != null ? local : LocalDateTime.now();
        return value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}

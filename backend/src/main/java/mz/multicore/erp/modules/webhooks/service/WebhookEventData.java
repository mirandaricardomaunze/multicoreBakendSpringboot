package mz.multicore.erp.modules.webhooks.service;

import mz.multicore.erp.modules.webhooks.model.WebhookEventType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Summary of the resource carried in {@code data} of each webhook event (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1,
 * WH-02). Only numbers, totals, SKU and the customer's name/NUIT — never phones, emails, addresses, costs or margins.
 */
sealed interface WebhookEventData {

    WebhookEventType type();

    /** {@code invoice.issued}: invoice, invoice-receipt or POS sale. */
    record InvoiceIssued(String invoiceNumber, LocalDate documentDate, String channel, WebhookCustomer customer,
                         BigDecimal totalBeforeTax, BigDecimal taxAmount, BigDecimal totalAmount,
                         BigDecimal amountPaid) implements WebhookEventData {
        @Override
        public WebhookEventType type() {
            return WebhookEventType.INVOICE_ISSUED;
        }
    }

    /** {@code credit_note.issued}: credit note approved. */
    record CreditNoteIssued(String creditNoteNumber, String invoiceNumber, WebhookCustomer customer,
                            BigDecimal totalBeforeTax, BigDecimal taxAmount,
                            BigDecimal totalAmount) implements WebhookEventData {
        @Override
        public WebhookEventType type() {
            return WebhookEventType.CREDIT_NOTE_ISSUED;
        }
    }

    /** {@code order.status_changed}: statuses are {@code OrderStatus} names. */
    record OrderStatusChanged(String orderNumber, String previousStatus, String status, WebhookCustomer customer,
                              BigDecimal totalAmount, String invoiceNumber) implements WebhookEventData {
        @Override
        public WebhookEventType type() {
            return WebhookEventType.ORDER_STATUS_CHANGED;
        }
    }

    /** {@code stock.availability_changed}: {@code status} is {@code OUT_OF_STOCK} or {@code BACK_IN_STOCK}. */
    record StockAvailabilityChanged(String sku, String productName, String warehouseCode, String warehouseName,
                                    BigDecimal available, String status) implements WebhookEventData {
        static final String OUT_OF_STOCK = "OUT_OF_STOCK";
        static final String BACK_IN_STOCK = "BACK_IN_STOCK";

        @Override
        public WebhookEventType type() {
            return WebhookEventType.STOCK_AVAILABILITY_CHANGED;
        }
    }
}

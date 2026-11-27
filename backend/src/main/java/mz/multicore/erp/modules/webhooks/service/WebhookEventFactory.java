package mz.multicore.erp.modules.webhooks.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.SecretTokens;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.CreditNoteIssued;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.InvoiceIssued;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.OrderStatusChanged;
import mz.multicore.erp.modules.webhooks.service.WebhookEventData.StockAvailabilityChanged;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JSON body of a webhook event (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2.1, WH-02):
 * {@code {"id": "evt_…", "type", "occurredAt", "companyId", "data"}} with camelCase keys, ISO 8601 dates with offset
 * and money with 2 decimal places as numbers. Pure — no HTTP and no persistence.
 */
@Component
public class WebhookEventFactory {

    static final String EVENT_ID_PREFIX = "evt_";

    private final ObjectMapper mapper = new ObjectMapper().enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    WebhookEventPayload build(Long companyId, OffsetDateTime occurredAt, WebhookEventData data) {
        String id = SecretTokens.generate(EVENT_ID_PREFIX);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", id);
        envelope.put("type", data.type().code());
        envelope.put("occurredAt", occurredAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        envelope.put("companyId", companyId);
        envelope.put("data", data(data));
        try {
            return new WebhookEventPayload(id, data.type(), mapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corpo do webhook " + data.type().code() + " não serializável.", e);
        }
    }

    private static Map<String, Object> data(WebhookEventData data) {
        Map<String, Object> body = new LinkedHashMap<>();
        switch (data) {
            case InvoiceIssued d -> {
                body.put("invoiceNumber", d.invoiceNumber());
                body.put("documentDate", date(d.documentDate()));
                body.put("channel", d.channel());
                body.put("customer", customer(d.customer()));
                body.put("totalBeforeTax", money(d.totalBeforeTax()));
                body.put("taxAmount", money(d.taxAmount()));
                body.put("totalAmount", money(d.totalAmount()));
                body.put("amountPaid", money(d.amountPaid()));
            }
            case CreditNoteIssued d -> {
                body.put("creditNoteNumber", d.creditNoteNumber());
                body.put("invoiceNumber", d.invoiceNumber());
                body.put("customer", customer(d.customer()));
                body.put("totalBeforeTax", money(d.totalBeforeTax()));
                body.put("taxAmount", money(d.taxAmount()));
                body.put("totalAmount", money(d.totalAmount()));
            }
            case OrderStatusChanged d -> {
                body.put("orderNumber", d.orderNumber());
                body.put("previousStatus", d.previousStatus());
                body.put("status", d.status());
                body.put("customer", customer(d.customer()));
                body.put("totalAmount", money(d.totalAmount()));
                body.put("invoiceNumber", d.invoiceNumber());
            }
            case StockAvailabilityChanged d -> {
                body.put("sku", d.sku());
                body.put("productName", d.productName());
                body.put("warehouseCode", d.warehouseCode());
                body.put("warehouseName", d.warehouseName());
                body.put("available", quantity(d.available()));
                body.put("status", d.status());
            }
        }
        return body;
    }

    private static Map<String, Object> customer(WebhookCustomer customer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", customer == null ? null : customer.name());
        body.put("nuit", customer == null ? null : customer.nuit());
        return body;
    }

    private static String date(LocalDate date) {
        return date == null ? null : date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal quantity(BigDecimal value) {
        BigDecimal safe = value == null || value.signum() < 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        return safe.scale() < 0 ? safe.setScale(0) : safe;
    }
}

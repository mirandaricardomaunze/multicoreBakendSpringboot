package mz.multicore.erp.modules.webhooks.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
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
import mz.multicore.erp.modules.webhooks.model.WebhookEventType;
import mz.multicore.erp.modules.webhooks.model.WebhookSubscription;
import mz.multicore.erp.modules.webhooks.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness WH-02 (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.2, §4.2.1): the four events are built from domain events with
 * the envelope {@code id}/{@code type}/{@code occurredAt}/{@code companyId}/{@code data}; {@code data} carries no
 * personal data beyond the customer's name/NUIT; only active subscriptions of the company that chose the type
 * receive it; an unchanged order status or stock that does not cross zero builds nothing.
 */
class WebhookEventTest {

    private static final Long COMPANY_ID = 1L;
    private static final OffsetDateTime AT = OffsetDateTime.of(2026, 11, 27, 10, 15, 0, 0, ZoneOffset.ofHours(2));

    private final WebhookSubscriptionRepository repository = mock(WebhookSubscriptionRepository.class);
    private final InvoiceLookup invoiceLookup = mock(InvoiceLookup.class);
    private final InvoiceNoteLookup noteLookup = mock(InvoiceNoteLookup.class);
    private final OrderLookup orderLookup = mock(OrderLookup.class);
    /** Keeps the scale of decimals so {@code 116.00} is read back as written. */
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);
    private final List<WebhookSubscription> subscriptions = new ArrayList<>();

    private final WebhookEventService service = new WebhookEventService(repository,
            new WebhookDomainEventTranslator(invoiceLookup, noteLookup, orderLookup), new WebhookEventFactory());

    @BeforeEach
    void setUp() {
        when(repository.findByCompanyIdAndActiveTrue(any())).thenAnswer(invocation -> subscriptions.stream()
                .filter(WebhookSubscription::isActive)
                .filter(s -> s.getCompany().getId().equals(invocation.getArgument(0)))
                .toList());
    }

    @Test
    void invoiceIssuedCarriesTheInvoiceSummaryAndTheEnvelope() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        Invoice invoice = invoice(55L, "FT 2026/00012", SalesChannel.POS, client("Loja Alfa, Lda", "400123456"));
        invoice.setCustomerName(null);

        List<PreparedWebhookEvent> prepared = service.prepare(sale(55L));

        assertThat(prepared).hasSize(1);
        JsonNode body = json.readTree(prepared.get(0).payload().body());
        assertThat(fieldNames(body)).containsExactly("id", "type", "occurredAt", "companyId", "data");
        assertThat(body.get("id").asText()).startsWith("evt_").hasSize(4 + 43);
        assertThat(body.get("type").asText()).isEqualTo("invoice.issued");
        assertThat(OffsetDateTime.parse(body.get("occurredAt").asText())).isNotNull();
        assertThat(body.get("companyId").asLong()).isEqualTo(COMPANY_ID);
        JsonNode data = body.get("data");
        assertThat(fieldNames(data)).containsExactly("invoiceNumber", "documentDate", "channel", "customer",
                "totalBeforeTax", "taxAmount", "totalAmount", "amountPaid");
        assertThat(data.get("invoiceNumber").asText()).isEqualTo("FT 2026/00012");
        assertThat(data.get("documentDate").asText()).isEqualTo("2026-11-27");
        assertThat(data.get("channel").asText()).isEqualTo("POS");
        assertThat(data.get("totalBeforeTax").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(data.get("totalAmount").toString()).isEqualTo("116.00");
        assertThat(data.get("taxAmount").toString()).isEqualTo("16.00");
        assertThat(data.get("amountPaid").toString()).isEqualTo("50.00");
        assertThat(fieldNames(data.get("customer"))).containsExactly("name", "nuit");
        assertThat(data.get("customer").get("name").asText()).isEqualTo("Loja Alfa, Lda");
        assertThat(data.get("customer").get("nuit").asText()).isEqualTo("400123456");
        assertThat(prepared.get(0).payload().type()).isEqualTo(WebhookEventType.INVOICE_ISSUED);
    }

    @Test
    void manualAndOrderInvoicesGoAsManualAndWalkInUsesTheNameOnTheDocumentWithoutNuit() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        Invoice invoice = invoice(55L, "FT 2026/00013", SalesChannel.ORDER, client("Consumidor Final", "000000000"));
        invoice.setCustomerName("Maria Joana");

        JsonNode data = data(service.prepare(sale(55L)).get(0));

        assertThat(data.get("channel").asText()).isEqualTo("MANUAL");
        assertThat(data.get("customer").get("name").asText()).isEqualTo("Maria Joana");
        assertThat(data.get("customer").get("nuit").isNull()).isTrue();
    }

    @Test
    void dataNeverCarriesPhonesEmailsAddressesCostsOrMargins() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.allOf(WebhookEventType.class));
        Client client = client("Loja Alfa, Lda", "400123456");
        client.setEmail("compras@alfa.co.mz");
        client.setAddress("Av. 24 de Julho, Maputo");
        invoice(55L, "FT 2026/00012", SalesChannel.MANUAL, client);

        String body = service.prepare(sale(55L)).get(0).payload().body();

        assertThat(body).doesNotContain("compras@alfa.co.mz", "Av. 24 de Julho", "cost", "margin", "phone",
                "email", "address", "60.00");
    }

    @Test
    void creditNoteIssuedCarriesTheNoteAndItsInvoice() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.CREDIT_NOTE_ISSUED));
        Invoice invoice = invoice(55L, "FT 2026/00012", SalesChannel.MANUAL, client("Loja Alfa, Lda", "400123456"));
        CreditNote note = new CreditNote();
        note.setId(70L);
        note.setNoteNumber("NC 2026/00003");
        note.setInvoice(invoice);
        note.setClient(invoice.getClient());
        note.setApprovedAt(LocalDateTime.of(2026, 11, 27, 11, 0));
        when(noteLookup.findCreditNote(70L)).thenReturn(Optional.of(note));

        List<PreparedWebhookEvent> prepared = service.prepare(new CreditNoteApprovedEvent(COMPANY_ID, 70L,
                "NC 2026/00003", LocalDate.of(2026, 11, 27), new BigDecimal("10"), new BigDecimal("1.6"),
                new BigDecimal("11.6"), new BigDecimal("6")));

        JsonNode body = json.readTree(prepared.get(0).payload().body());
        assertThat(body.get("type").asText()).isEqualTo("credit_note.issued");
        JsonNode data = body.get("data");
        assertThat(fieldNames(data)).containsExactly("creditNoteNumber", "invoiceNumber", "customer",
                "totalBeforeTax", "taxAmount", "totalAmount");
        assertThat(data.get("creditNoteNumber").asText()).isEqualTo("NC 2026/00003");
        assertThat(data.get("invoiceNumber").asText()).isEqualTo("FT 2026/00012");
        assertThat(data.get("totalAmount").toString()).isEqualTo("11.60");
        assertThat(data.get("taxAmount").toString()).isEqualTo("1.60");
        assertThat(data.get("customer").get("nuit").asText()).isEqualTo("400123456");
    }

    @Test
    void orderStatusChangedCarriesBothStatusesTotalAndInvoiceNumber() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.ORDER_STATUS_CHANGED));
        Order order = order(90L, "ENC-2026/00001", client("Loja Alfa, Lda", "400123456"));
        order.setInvoiceId(55L);
        invoice(55L, "FT 2026/00012", SalesChannel.ORDER, order.getClient());

        List<PreparedWebhookEvent> prepared = service.prepare(new OrderStatusChangedEvent(COMPANY_ID, 90L,
                "ENC-2026/00001", "PENDING", "BILLED", AT));

        JsonNode body = json.readTree(prepared.get(0).payload().body());
        assertThat(body.get("type").asText()).isEqualTo("order.status_changed");
        assertThat(OffsetDateTime.parse(body.get("occurredAt").asText())).isEqualTo(AT);
        JsonNode data = body.get("data");
        assertThat(fieldNames(data)).containsExactly("orderNumber", "previousStatus", "status", "customer",
                "totalAmount", "invoiceNumber");
        assertThat(data.get("orderNumber").asText()).isEqualTo("ENC-2026/00001");
        assertThat(data.get("previousStatus").asText()).isEqualTo("PENDING");
        assertThat(data.get("status").asText()).isEqualTo("BILLED");
        assertThat(data.get("totalAmount").toString()).isEqualTo("232.00");
        assertThat(data.get("invoiceNumber").asText()).isEqualTo("FT 2026/00012");
    }

    @Test
    void orderNotYetBilledHasNullInvoiceAndWalkInHasNoNuit() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.ORDER_STATUS_CHANGED));
        Order order = order(90L, "ENC-2026/00002", client("Consumidor Final", "000000000"));
        order.setWalkInName("Carlos Mondlane");

        JsonNode data = data(service.prepare(new OrderStatusChangedEvent(COMPANY_ID, 90L, "ENC-2026/00002",
                "PENDING_APPROVAL", "PENDING", AT)).get(0));

        assertThat(data.get("invoiceNumber").isNull()).isTrue();
        assertThat(data.get("customer").get("name").asText()).isEqualTo("Carlos Mondlane");
        assertThat(data.get("customer").get("nuit").isNull()).isTrue();
    }

    @Test
    void sameOrderStatusBuildsNothing() {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.ORDER_STATUS_CHANGED));
        order(90L, "ENC-2026/00001", client("Loja Alfa, Lda", "400123456"));

        assertThat(service.prepare(new OrderStatusChangedEvent(COMPANY_ID, 90L, "ENC-2026/00001", "PENDING",
                "PENDING", AT))).isEmpty();
        verify(orderLookup, never()).findOrderForCompany(any(), any());
    }

    @Test
    void stockGoingOutAndComingBackCarriesSkuWarehouseAndAvailable() throws Exception {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED));

        JsonNode out = json.readTree(service.prepare(stock("5", "0")).get(0).payload().body());
        assertThat(out.get("type").asText()).isEqualTo("stock.availability_changed");
        JsonNode data = out.get("data");
        assertThat(fieldNames(data)).containsExactly("sku", "productName", "warehouseCode", "warehouseName",
                "available", "status");
        assertThat(data.get("sku").asText()).isEqualTo("ARZ-25");
        assertThat(data.get("productName").asText()).isEqualTo("Arroz 25 kg");
        assertThat(data.get("warehouseCode").asText()).isEqualTo("A01");
        assertThat(data.get("warehouseName").asText()).isEqualTo("Loja Central");
        assertThat(data.get("available").decimalValue()).isEqualByComparingTo("0");
        assertThat(data.get("status").asText()).isEqualTo("OUT_OF_STOCK");

        JsonNode back = data(service.prepare(stock("0", "12.500")).get(0));
        assertThat(back.get("status").asText()).isEqualTo("BACK_IN_STOCK");
        assertThat(back.get("available").toString()).isEqualTo("12.5");
    }

    @Test
    void stockThatDoesNotCrossZeroBuildsNothing() {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.STOCK_AVAILABILITY_CHANGED));

        assertThat(service.prepare(stock("5", "3"))).isEmpty();
        assertThat(service.prepare(stock("0", "0"))).isEmpty();
    }

    @Test
    void onlyActiveSubscriptionsOfTheCompanyThatChoseTheTypeReceiveTheSameBody() {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        subscribe(11L, COMPANY_ID, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED,
                WebhookEventType.ORDER_STATUS_CHANGED));
        subscribe(12L, COMPANY_ID, false, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        subscribe(13L, COMPANY_ID, true, EnumSet.of(WebhookEventType.CREDIT_NOTE_ISSUED));
        subscribe(14L, 2L, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        invoice(55L, "FT 2026/00012", SalesChannel.MANUAL, client("Loja Alfa, Lda", "400123456"));

        List<PreparedWebhookEvent> prepared = service.prepare(sale(55L));

        assertThat(prepared).extracting(PreparedWebhookEvent::subscriptionId).containsExactlyInAnyOrder(10L, 11L);
        assertThat(prepared.get(0).payload()).isSameAs(prepared.get(1).payload());
        assertThat(prepared).extracting(PreparedWebhookEvent::url).allMatch(url -> url.startsWith("https://"));
    }

    @Test
    void withoutInterestedSubscriptionsNoBodyIsBuilt() {
        subscribe(13L, COMPANY_ID, true, EnumSet.of(WebhookEventType.CREDIT_NOTE_ISSUED));

        assertThat(service.prepare(sale(55L))).isEmpty();
        verify(invoiceLookup, never()).find(any());
    }

    @Test
    void eachOccurrenceGetsItsOwnEventId() {
        subscribe(10L, COMPANY_ID, true, EnumSet.of(WebhookEventType.INVOICE_ISSUED));
        invoice(55L, "FT 2026/00012", SalesChannel.MANUAL, client("Loja Alfa, Lda", "400123456"));

        String first = service.prepare(sale(55L)).get(0).payload().eventId();
        String second = service.prepare(sale(55L)).get(0).payload().eventId();

        assertThat(first).startsWith("evt_").isNotEqualTo(second);
    }

    // ---- helpers ----

    private void subscribe(Long id, Long companyId, boolean active, Set<WebhookEventType> events) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setId(id);
        mz.multicore.erp.modules.company.model.Company company = new mz.multicore.erp.modules.company.model.Company();
        company.setId(companyId);
        subscription.setCompany(company);
        subscription.setUrl("https://loja.example.com/hooks/" + id);
        subscription.setEvents(events);
        subscription.setActive(active);
        subscriptions.add(subscription);
    }

    private static Client client(String name, String taxId) {
        Client client = new Client();
        client.setName(name);
        client.setTaxId(taxId);
        return client;
    }

    private Invoice invoice(Long id, String number, SalesChannel channel, Client client) {
        Invoice invoice = new Invoice();
        invoice.setId(id);
        invoice.setInvoiceNumber(number);
        invoice.setSalesChannel(channel);
        invoice.setClient(client);
        invoice.setDocumentDate(LocalDate.of(2026, 11, 27));
        invoice.setCreatedAt(LocalDateTime.of(2026, 11, 27, 10, 15));
        when(invoiceLookup.find(id)).thenReturn(Optional.of(invoice));
        return invoice;
    }

    private Order order(Long id, String number, Client client) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(number);
        order.setClient(client);
        order.setTotalAmount(new BigDecimal("232"));
        when(orderLookup.findOrderForCompany(id, COMPANY_ID)).thenReturn(Optional.of(order));
        return order;
    }

    private static SaleRegisteredEvent sale(Long invoiceId) {
        return new SaleRegisteredEvent(COMPANY_ID, invoiceId, "FT 2026/00012", LocalDate.of(2026, 11, 27),
                new BigDecimal("100"), new BigDecimal("16"), new BigDecimal("116"), new BigDecimal("60"),
                new BigDecimal("50"), true);
    }

    private static StockAvailabilityChangedEvent stock(String before, String after) {
        return new StockAvailabilityChangedEvent(COMPANY_ID, "ARZ-25", "Arroz 25 kg", "A01", "Loja Central",
                new BigDecimal(before), new BigDecimal(after), AT);
    }

    private JsonNode data(PreparedWebhookEvent prepared) throws Exception {
        return json.readTree(prepared.payload().body()).get("data");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        iterator.forEachRemaining(names::add);
        return names;
    }
}

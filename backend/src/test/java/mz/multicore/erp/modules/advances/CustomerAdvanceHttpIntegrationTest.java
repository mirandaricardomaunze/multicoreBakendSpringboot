package mz.multicore.erp.modules.advances;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Customer advances end to end over HTTP ({@code docs/ADIANTAMENTOS_SPEC.md}): the cash enters once, VAT ends up
 * as the VAT of the final invoice, account 2101 and the customer statement show only what is still owed.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:advances;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerAdvanceHttpIntegrationTest {

    private static final BigDecimal ADVANCE = new BigDecimal("3000.00");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId, long otherClientId,
                       long walkInId, long treasuryId) {}

    @Test
    void advanceIsInvoicedReceivedOnceAndOffsetOnTheFinalInvoice() throws Exception {
        Ctx c = context();
        BigDecimal cashBefore = treasuryBalance(c);
        BigDecimal ledgerBefore = clientsLedger(c);
        BigDecimal statementBefore = statementBalance(c, c.clientId());

        JsonNode advance = receive(c, c.clientId(), ADVANCE, "adv-" + UUID.randomUUID());
        assertThat(advance.get("status").asText()).isEqualTo("OPEN");
        assertThat(money(advance, "amount")).isEqualByComparingTo(ADVANCE);
        assertThat(advance.get("advanceInvoiceNumber").asText()).startsWith("FT");
        assertThat(advance.get("receiptNumber").asText()).startsWith("RC");
        assertThat(treasuryBalance(c).subtract(cashBefore)).isEqualByComparingTo(ADVANCE);

        long finalInvoiceId = finalInvoice(c, c.clientId());
        BigDecimal finalTotal = money(invoice(c, finalInvoiceId), "totalAmount");

        // Wrong client and an advance invoice as target are refused before anything is issued.
        long foreignInvoice = finalInvoice(c, c.otherClientId());
        apply(c, advance.get("id").asLong(), foreignInvoice).andExpect(status().isBadRequest());
        apply(c, advance.get("id").asLong(), advance.get("advanceInvoiceId").asLong()).andExpect(status().isBadRequest());

        JsonNode applied = json(apply(c, advance.get("id").asLong(), finalInvoiceId));
        assertThat(applied.get("status").asText()).isEqualTo("APPLIED");
        assertThat(applied.get("creditNoteNumber").asText()).startsWith("NC");
        assertThat(applied.get("offsetReceiptNumber").asText()).startsWith("RC");
        apply(c, advance.get("id").asLong(), finalInvoiceId).andExpect(status().isBadRequest());

        JsonNode target = invoice(c, finalInvoiceId);
        assertThat(money(target, "amountPaid")).isEqualByComparingTo(ADVANCE);
        assertThat(target.get("status").asText()).isEqualTo("PARTIALLY_PAID");

        BigDecimal owed = finalTotal.subtract(ADVANCE);
        assertThat(treasuryBalance(c).subtract(cashBefore)).as("cash enters only once").isEqualByComparingTo(ADVANCE);
        BigDecimal foreignTotal = money(invoice(c, foreignInvoice), "totalAmount");
        assertThat(clientsLedger(c).subtract(ledgerBefore)).as("account 2101 (both clients)").isEqualByComparingTo(owed.add(foreignTotal));
        assertThat(statementBalance(c, c.clientId()).subtract(statementBefore)).as("customer statement").isEqualByComparingTo(owed);
        JsonNode trial = getJson("/api/accounting/trial-balance?from=2000-01-01&to=" + LocalDate.now().plusDays(1), c);
        assertThat(money(trial, "totalDebit")).isEqualByComparingTo(money(trial, "totalCredit"));

        // Neither the original receipt nor the offset can be cancelled: that would give back money twice.
        cancelReceipt(c, applied.get("receiptId").asLong()).andExpect(status().isBadRequest());
        long offsetId = receiptId(c, applied.get("offsetReceiptNumber").asText());
        cancelReceipt(c, offsetId).andExpect(status().isBadRequest());
    }

    @Test
    void retryReturnsTheSameAdvanceAndTheSystemProductStaysInsideTheModule() throws Exception {
        Ctx c = context();
        String key = "adv-" + UUID.randomUUID();
        JsonNode first = receive(c, c.clientId(), new BigDecimal("1160.00"), key);
        JsonNode retry = receive(c, c.clientId(), new BigDecimal("1160.00"), key);
        assertThat(retry.get("id").asLong()).isEqualTo(first.get("id").asLong());

        if (c.walkInId() > 0) {
            send(post("/api/advances"), c, receiveBody(c, c.walkInId(), ADVANCE, "adv-" + UUID.randomUUID()))
                    .andExpect(status().isBadRequest());
        }

        long systemProduct = systemProductId(c);
        send(post("/api/comercial/orders"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,"lines":[{"productId":%d,"quantity":1,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), systemProduct)).andExpect(status().isBadRequest());
        send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,"lines":[{"productId":%d,"quantity":1,"taxRate":0.16}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), systemProduct)).andExpect(status().isBadRequest());
    }

    private JsonNode receive(Ctx c, long clientId, BigDecimal amount, String key) throws Exception {
        return json(send(post("/api/advances"), c, receiveBody(c, clientId, amount, key)));
    }

    private String receiveBody(Ctx c, long clientId, BigDecimal amount, String key) {
        return """
                {"companyId":%s,"clientId":%d,"warehouseId":%d,"treasuryAccountId":%d,"paymentMethod":"CASH",
                 "amount":%s,"notes":"Sinal do bolo de aniversário","requestKey":"%s"}"""
                .formatted(c.companyId(), clientId, c.warehouseId(), c.treasuryId(), amount.toPlainString(), key);
    }

    private ResultActions apply(Ctx c, long advanceId, long invoiceId) throws Exception {
        return send(post("/api/advances/" + advanceId + "/apply"), c,
                "{\"companyId\":%s,\"invoiceId\":%d}".formatted(c.companyId(), invoiceId));
    }

    private ResultActions cancelReceipt(Ctx c, long receiptId) throws Exception {
        return send(post("/api/comercial/receipts/" + receiptId + "/cancel"), c, "{\"reason\":\"Teste de anulação\"}");
    }

    private long finalInvoice(Ctx c, long clientId) throws Exception {
        long productId = json(send(post("/api/comercial/products"), c, """
                {"sku":"BOLO-%s","name":"Bolo de aniversário (teste)","unitPrice":1000,"purchasePrice":1,"minStock":0,
                 "unitsPerBox":1,"saleType":"UNIT","stockTracked":false}""".formatted(UUID.randomUUID().toString().substring(0, 8))))
                .get("id").asLong();
        return json(send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,"lines":[{"productId":%d,"quantity":10,"taxRate":0.16}]}"""
                .formatted(clientId, c.companyId(), c.warehouseId(), productId))).get("id").asLong();
    }

    private JsonNode invoice(Ctx c, long id) throws Exception {
        for (JsonNode invoice : getJson("/api/comercial/invoices?companyId=" + c.companyId(), c)) {
            if (invoice.get("id").asLong() == id) return invoice;
        }
        throw new IllegalStateException("Invoice " + id + " not listed");
    }

    private long receiptId(Ctx c, String number) throws Exception {
        for (JsonNode receipt : getJson("/api/comercial/receipts?companyId=" + c.companyId(), c)) {
            if (number.equals(receipt.get("receiptNumber").asText())) return receipt.get("id").asLong();
        }
        throw new IllegalStateException("Receipt " + number + " not listed");
    }

    private long systemProductId(Ctx c) throws Exception {
        String sku = AdvancePayments.systemSku(Long.valueOf(c.companyId()));
        for (JsonNode product : getJson("/api/comercial/products", c)) {
            if (sku.equals(product.path("sku").asText())) return product.get("id").asLong();
        }
        throw new IllegalStateException("System product not created");
    }

    private BigDecimal treasuryBalance(Ctx c) throws Exception {
        for (JsonNode account : getJson("/api/finance/accounts", c)) {
            if (account.get("id").asLong() == c.treasuryId()) return money(account, "balance");
        }
        throw new IllegalStateException("Treasury account not listed");
    }

    private BigDecimal clientsLedger(Ctx c) throws Exception {
        return money(getJson("/api/accounting/ledger/2101?from=2000-01-01&to=" + LocalDate.now().plusDays(1), c), "closingBalance");
    }

    private BigDecimal statementBalance(Ctx c, long clientId) throws Exception {
        return money(getJson("/api/comercial/statements/customer?clientId=" + clientId
                + "&startDate=2000-01-01&endDate=" + LocalDate.now().plusDays(1), c), "closingBalance");
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Ctx probe = new Ctx(token, companyId, 0, 0, 0, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            JsonNode accounts = getJson("/api/finance/accounts", probe);
            long first = 0, second = 0, walkIn = 0;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if ("000000000".equals(client.path("taxId").asText())) walkIn = client.get("id").asLong();
                else if (first == 0) first = client.get("id").asLong();
                else if (second == 0) second = client.get("id").asLong();
            }
            if (warehouses.isEmpty() || accounts.isEmpty() || second == 0) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), first, second, walkIn,
                    accounts.get(0).get("id").asLong());
        }
        throw new IllegalStateException("No company with a warehouse, a treasury account and two named clients in the seed.");
    }

    private static BigDecimal money(JsonNode node, String field) {
        return new BigDecimal(node.get(field).asText());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Ctx c) {
        return req.header("Authorization", "Bearer " + c.token()).header("X-Company-Id", c.companyId());
    }

    private JsonNode getJson(String path, Ctx c) throws Exception {
        return read(mockMvc.perform(auth(get(path), c)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Ctx c, String body) throws Exception {
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(body.isEmpty() ? "{}" : body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}

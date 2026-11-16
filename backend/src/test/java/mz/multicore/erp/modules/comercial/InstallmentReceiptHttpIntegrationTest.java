package mz.multicore.erp.modules.comercial;

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
 * Money for an invoice always enters through a receipt (RC): paying an instalment issues the receipt and the plan
 * follows it; cancelling the receipt puts the instalment back. The treasury "pay invoice" also issues a receipt.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:installmentreceipt;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstallmentReceiptHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void instalmentsAndTreasurySettlementGoThroughReceipts() throws Exception {
        Ctx c = context();
        long account = getJson("/api/finance/accounts", c).get(0).get("id").asLong();
        JsonNode invoice = invoice(c, 4);
        long invoiceId = invoice.get("id").asLong();
        String path = "/api/comercial/invoices/" + invoiceId + "/installments";
        json(send(post(path), c, """
                {"invoiceId":%d,"numberOfInstallments":2,"intervalDays":30,"firstDueDate":"%s","customAmounts":[]}"""
                .formatted(invoiceId, LocalDate.now().plusDays(30))));

        int receiptsBefore = receiptsFor(c, invoice.get("invoiceNumber").asText());
        BigDecimal half = new BigDecimal(invoice.get("totalAmount").asText()).divide(new BigDecimal("2"));
        JsonNode plan = json(send(post(path + "/pay?amount=" + half + "&financeAccountId=" + account), c, ""));
        assertThat(plan.get(0).get("status").asText()).isEqualTo("PAID");
        assertThat(plan.get(1).get("status").asText()).isNotEqualTo("PAID");
        assertThat(receiptsFor(c, invoice.get("invoiceNumber").asText())).isEqualTo(receiptsBefore + 1);

        long receiptId = 0;
        for (JsonNode r : getJson("/api/comercial/receipts?companyId=" + c.companyId(), c)) {
            if (invoice.get("invoiceNumber").asText().equals(r.get("invoiceNumber").asText())) receiptId = r.get("id").asLong();
        }
        send(post("/api/comercial/receipts/" + receiptId + "/cancel"), c, "{\"reason\":\"Engano\"}")
                .andExpect(status().isNoContent());
        assertThat(getJson(path, c).get(0).get("status").asText()).isNotEqualTo("PAID");

        // Treasury settlement of another invoice: a receipt appears.
        JsonNode other = invoice(c, 1);
        send(post("/api/finance/pay-invoice"), c, """
                {"invoiceId":%d,"accountId":%d}""".formatted(other.get("id").asLong(), account)).andExpect(status().isOk());
        assertThat(receiptsFor(c, other.get("invoiceNumber").asText())).isEqualTo(1);
    }

    private JsonNode invoice(Ctx c, int qty) throws Exception {
        long product = json(send(post("/api/comercial/products"), c, """
                {"sku":"IR-%s","name":"Bloco 15 (teste prestacoes)","unitPrice":1000,"purchasePrice":600,"minStock":0,
                 "unitsPerBox":1,"saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8))))
                .get("id").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":20,"movementType":"ENTRY"}""".formatted(product, c.warehouseId()))
                .andExpect(status().isOk());
        return json(send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":%d,"taxRate":0,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), product, qty)));
    }

    private int receiptsFor(Ctx c, String invoiceNumber) throws Exception {
        int count = 0;
        for (JsonNode r : getJson("/api/comercial/receipts?companyId=" + c.companyId(), c)) {
            if (invoiceNumber.equals(r.get("invoiceNumber").asText()) && !"CANCELLED".equals(r.get("status").asText())) count++;
        }
        return count;
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Ctx probe = new Ctx(token, companyId, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            JsonNode clients = getJson("/api/comercial/clients", probe);
            if (!warehouses.isEmpty() && !clients.isEmpty()) {
                return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), clients.get(0).get("id").asLong());
            }
        }
        throw new IllegalStateException("No company with a sales warehouse and a client in the seed.");
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
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}

package mz.multicore.erp.modules.pos;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cash received at the counter for a receipt enters the drawer of the operator's open POS session, so the close
 * count matches the cash physically there; cancelling the receipt takes it out again (MOVIMENTOS_COMERCIAIS.md §7.10).
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:counterreceipt;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounterReceiptTillHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void cashReceiptAtTheCounterGoesIntoTheDrawer() throws Exception {
        Ctx c = context();
        long product = json(send(post("/api/comercial/products"), c, """
                {"sku":"CR-%s","name":"Saco de cimento (teste balcao)","unitPrice":1000,"purchasePrice":700,"minStock":0,
                 "unitsPerBox":1,"saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8))))
                .get("id").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":10,"movementType":"ENTRY"}""".formatted(product, c.warehouseId()))
                .andExpect(status().isOk());
        long invoice = json(send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":2,"taxRate":0,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), product))).get("id").asLong();
        long session = json(send(post("/api/pos/sessions/open"), c,
                "{\"operator\":\"ana\",\"openingBalance\":100,\"companyId\":" + c.companyId() + "}")).get("id").asLong();
        long account = getJson("/api/finance/accounts", c).get(0).get("id").asLong();

        long receipt = json(send(post("/api/comercial/receipts"), c, """
                {"invoiceId":%d,"treasuryAccountId":%d,"paymentMethod":"CASH","amountPaid":500}"""
                .formatted(invoice, account))).get("id").asLong();
        JsonNode z = getJson("/api/pos/sessions/" + session + "/z-report", c);
        assertThat(new BigDecimal(z.get("counterReceipts").asText())).isEqualByComparingTo("500");
        assertThat(new BigDecimal(z.get("expectedCash").asText())).isEqualByComparingTo("600");

        // A bank transfer does not touch the drawer.
        json(send(post("/api/comercial/receipts"), c, """
                {"invoiceId":%d,"treasuryAccountId":%d,"paymentMethod":"BANK_TRANSFER","amountPaid":300}"""
                .formatted(invoice, account)));
        assertThat(new BigDecimal(getJson("/api/pos/sessions/" + session + "/z-report", c).get("expectedCash").asText()))
                .isEqualByComparingTo("600");

        send(post("/api/comercial/receipts/" + receipt + "/cancel"), c, "{\"reason\":\"Engano no valor\"}")
                .andExpect(status().isNoContent());
        z = getJson("/api/pos/sessions/" + session + "/z-report", c);
        assertThat(new BigDecimal(z.get("counterReceipts").asText())).isEqualByComparingTo("0");
        assertThat(new BigDecimal(z.get("expectedCash").asText())).isEqualByComparingTo("100");

        // Counter receipts are never typed by hand at the till.
        send(post("/api/pos/sessions/" + session + "/movements"), c,
                "{\"type\":\"COUNTER_RECEIPT\",\"amount\":50,\"description\":\"manual\"}")
                .andExpect(status().is4xxClientError());
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

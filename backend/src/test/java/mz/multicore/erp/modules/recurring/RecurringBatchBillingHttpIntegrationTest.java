package mz.multicore.erp.modules.recurring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Batch billing of recurring contracts issues real commercial invoices, once per overdue period. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:recurring-billing;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecurringBatchBillingHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    @Test
    @DisplayName("Cada período vencido gera uma fatura com IVA; repetir o lote não fatura de novo; o contrato expira")
    void overduePeriodsBecomeInvoicesOnce() throws Exception {
        String[] ctx = login();
        long clientId = createClient(ctx);
        JsonNode contract = send(ctx, "/api/recurring-contracts", Map.of(
                "contractNumber", "AV-" + ThreadLocalRandom.current().nextInt(100000, 999999),
                "customerId", clientId,
                "description", "Limpeza mensal do escritório",
                "frequency", "MONTHLY",
                "billingDayOfMonth", 5,
                "fixedAmount", new BigDecimal("10000.00"),
                "startDate", "2026-01-01",
                "endDate", "2026-03-31",
                "autoRenew", false));

        JsonNode run = send(ctx, "/api/recurring-contracts/batch-billing?referenceDate=2026-03-10", Map.of());
        assertThat(run.get("errors")).as(run.toString()).isEmpty();
        assertThat(run.get("invoicesGenerated").asInt()).isEqualTo(3);
        assertThat(run.get("totalBilledAmount").decimalValue()).isEqualByComparingTo("34800.00");

        List<String> numbers = new ArrayList<>();
        run.get("generatedInvoiceNumbers").forEach(n -> numbers.add(n.asText().split(" ")[0]));
        JsonNode invoices = om.readTree(body(auth(get("/api/comercial/invoices"), ctx)));
        int matched = 0;
        for (JsonNode invoice : invoices) {
            if (!numbers.contains(invoice.get("invoiceNumber").asText())) continue;
            matched++;
            assertThat(invoice.get("clientId").asLong()).isEqualTo(clientId);
            assertThat(invoice.get("totalBeforeTax").decimalValue()).isEqualByComparingTo("10000.00");
            assertThat(invoice.get("totalAmount").decimalValue()).isEqualByComparingTo("11600.00");
        }
        assertThat(matched).isEqualTo(3);

        JsonNode again = send(ctx, "/api/recurring-contracts/batch-billing?referenceDate=2026-03-10", Map.of());
        assertThat(again.get("invoicesGenerated").asInt()).isZero();

        JsonNode after = om.readTree(body(auth(get("/api/recurring-contracts/" + contract.get("id").asLong()), ctx)));
        assertThat(after.get("status").asText()).isEqualTo("EXPIRED");
        assertThat(after.get("lastBilledDate").asText()).isEqualTo("2026-03-05");
    }

    private long createClient(String[] ctx) throws Exception {
        String nuit = String.valueOf(ThreadLocalRandom.current().nextInt(400000000, 499999999));
        return send(ctx, "/api/comercial/clients", Map.of(
                "name", "Cliente Avença " + nuit, "taxId", nuit, "email", "avenca" + nuit + "@exemplo.co.mz",
                "address", "Maputo", "paymentTermsDays", 30)).get("id").asLong();
    }

    private JsonNode send(String[] ctx, String url, Map<String, ?> payload) throws Exception {
        var result = mockMvc.perform(auth(post(url), ctx)
                .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(payload))).andReturn();
        String text = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(url + " -> " + text).isBetween(200, 201);
        return om.readTree(text);
    }

    private String body(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private String[] login() throws Exception {
        JsonNode login = om.readTree(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        return new String[]{login.get("token").asText(), login.get("companies").get(0).get("id").asText()};
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String[] ctx) {
        return req.header("Authorization", "Bearer " + ctx[0]).header("X-Company-Id", ctx[1]);
    }
}

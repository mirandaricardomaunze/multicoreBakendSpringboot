package mz.multicore.erp.modules.consignment;

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

/** Consignment delivery and settlement end to end ({@code docs/CONSIGNACAO_ROTA_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:consignment;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsignmentHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId, long walkInId) {}

    @Test
    void breadLeftAtResellerIsSettledReturnsComeBackAndOnlyTheSoldPartIsInvoiced() throws Exception {
        Ctx c = context();
        long bread = json(send(post("/api/comercial/products"), c, """
                {"sku":"CG-%s","name":"Pão (teste consignação)","unitPrice":10,"purchasePrice":4,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8)))).get("id").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":300,"movementType":"ENTRY"}""".formatted(bread, c.warehouseId()))
                .andExpect(status().isOk());

        send(post("/api/consignments"), c, deliver(c, c.clientId(), bread, "500")).andExpect(status().isBadRequest());
        if (c.walkInId() > 0) send(post("/api/consignments"), c, deliver(c, c.walkInId(), bread, "10")).andExpect(status().isBadRequest());
        String body = deliver(c, c.clientId(), bread, "200");
        JsonNode delivery = json(send(post("/api/consignments"), c, body));
        assertThat(delivery.get("number").asText()).startsWith("CG");
        assertThat(json(send(post("/api/consignments"), c, body)).get("id").asLong()).isEqualTo(delivery.get("id").asLong());
        assertThat(stock(c, bread, c.warehouseId())).as("left the shop once").isEqualByComparingTo("100");

        long lineId = delivery.get("lines").get(0).get("id").asLong();
        send(post("/api/consignments/" + delivery.get("id").asLong() + "/settle"), c, settle(c, lineId, "150", "60"))
                .andExpect(status().isBadRequest());
        JsonNode settled = json(send(post("/api/consignments/" + delivery.get("id").asLong() + "/settle"), c, settle(c, lineId, "30", "10")));
        assertThat(settled.get("status").asText()).isEqualTo("SETTLED");
        assertThat(new BigDecimal(settled.get("lines").get(0).get("sold").asText())).isEqualByComparingTo("160");
        assertThat(settled.get("invoiceNumber").asText()).startsWith("FT");
        // 160 sold x 10 MT + 16% VAT.
        assertThat(new BigDecimal(settled.get("invoiceTotal").asText())).isEqualByComparingTo("1856");
        assertThat(stock(c, bread, c.warehouseId())).as("good returns are back in the shop").isEqualByComparingTo("130");
        send(post("/api/consignments/" + delivery.get("id").asLong() + "/settle"), c, settle(c, lineId, "0", "0"))
                .andExpect(status().isBadRequest());
    }

    private String deliver(Ctx c, long clientId, long productId, String qty) {
        return """
                {"companyId":%s,"clientId":%d,"warehouseId":%d,"deliveryDate":"%s","route":"Rota Matola",
                 "lines":[{"productId":%d,"quantity":%s}],"requestKey":"cg-%s"}"""
                .formatted(c.companyId(), clientId, c.warehouseId(), LocalDate.now(), productId, qty, UUID.randomUUID());
    }

    private String settle(Ctx c, long lineId, String returned, String damaged) {
        return """
                {"companyId":%s,"lines":[{"lineId":%d,"returned":%s,"damaged":%s}]}""".formatted(c.companyId(), lineId, returned, damaged);
    }

    private BigDecimal stock(Ctx c, long productId, long warehouseId) throws Exception {
        for (JsonNode s : getJson("/api/inventory/stocks?warehouseId=" + warehouseId, c)) {
            if (s.get("productId").asLong() == productId) return new BigDecimal(s.get("quantity").asText());
        }
        return BigDecimal.ZERO;
    }

    private Ctx context() throws Exception {
        JsonNode login = read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Ctx probe = new Ctx(token, companyId, 0, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            Long named = null;
            long walkIn = 0;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if ("000000000".equals(client.path("taxId").asText())) walkIn = client.get("id").asLong();
                else if (named == null) named = client.get("id").asLong();
            }
            if (warehouses.isEmpty() || named == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), named, walkIn);
        }
        throw new IllegalStateException("No company with a sales warehouse and a named client in the seed.");
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

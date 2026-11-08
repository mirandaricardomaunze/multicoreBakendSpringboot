package mz.multicore.erp.modules.butchery;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carcass breakdown end to end over HTTP ({@code docs/DESMANCHA_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:butchery;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarcassBreakdownHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void carcassBecomesCutsWithCostPerKgYieldAndLoss() throws Exception {
        Ctx c = context();
        long carcass = product(c, "Boi inteiro (carcaça)", "0", "200");
        long fillet = product(c, "Lombo", "900", "0");
        long steak = product(c, "Bife da pá", "420", "0");
        long bones = product(c, "Ossos", "0", "0");
        entry(c, carcass, "250");

        JsonNode template = json(send(put("/api/butchery/templates"), c, """
                {"companyId":%s,"name":"Boi inteiro","sourceProductId":%d,"lossTolerancePct":8,"active":true,
                 "lines":[{"productId":%d,"expectedYieldPct":8},{"productId":%d,"expectedYieldPct":40},{"productId":%d,"expectedYieldPct":16}]}"""
                .formatted(c.companyId(), carcass, fillet, steak, bones)));
        assertThat(new BigDecimal(template.get("expectedYieldTotal").asText())).isEqualByComparingTo("64");

        String key = "dm-" + UUID.randomUUID();
        String body = """
                {"companyId":%s,"warehouseId":%d,"templateId":%d,"sourceProductId":%d,"inputKg":200,"inputCost":50000,
                 "reference":"Guia do matadouro 123","requestKey":"%s",
                 "lines":[{"productId":%d,"kg":20},{"productId":%d,"kg":100},{"productId":%d,"kg":40}]}"""
                .formatted(c.companyId(), c.warehouseId(), template.get("id").asLong(), carcass, key, fillet, steak, bones);
        JsonNode breakdown = json(send(post("/api/butchery/breakdowns"), c, body));
        assertThat(breakdown.get("number").asText()).startsWith("DM");
        assertThat(money(breakdown, "lossKg")).isEqualByComparingTo("40");
        assertThat(money(breakdown, "lossPct")).isEqualByComparingTo("20");
        assertThat(breakdown.get("lossAboveTolerance").asBoolean()).isTrue();
        BigDecimal allocated = BigDecimal.ZERO;
        for (JsonNode line : breakdown.get("lines")) allocated = allocated.add(money(line, "allocatedCost"));
        assertThat(allocated).isEqualByComparingTo("50000");
        assertThat(money(breakdown.get("lines").get(0), "costPerKg")).isEqualByComparingTo("750");
        assertThat(money(breakdown.get("lines").get(1), "costPerKg")).isEqualByComparingTo("350");
        assertThat(money(breakdown.get("lines").get(1), "realYieldPct")).isEqualByComparingTo("50");

        assertThat(stock(c, carcass)).isEqualByComparingTo("50");
        assertThat(stock(c, fillet)).isEqualByComparingTo("20");
        assertThat(stock(c, steak)).isEqualByComparingTo("100");

        JsonNode retry = json(send(post("/api/butchery/breakdowns"), c, body));
        assertThat(retry.get("id").asLong()).isEqualTo(breakdown.get("id").asLong());
        assertThat(stock(c, carcass)).as("a retry does not move stock twice").isEqualByComparingTo("50");

        send(post("/api/butchery/breakdowns"), c, """
                {"companyId":%s,"warehouseId":%d,"sourceProductId":%d,"inputKg":10,"requestKey":"%s",
                 "lines":[{"productId":%d,"kg":11}]}""".formatted(c.companyId(), c.warehouseId(), carcass,
                "dm-" + UUID.randomUUID(), fillet)).andExpect(status().isBadRequest());

        JsonNode cancelled = json(send(post("/api/butchery/breakdowns/" + breakdown.get("id").asLong() + "/cancel"), c,
                "{\"companyId\":%s,\"reason\":\"Pesagem errada\"}".formatted(c.companyId())));
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(stock(c, carcass)).isEqualByComparingTo("250");
        assertThat(stock(c, fillet)).isEqualByComparingTo("0");
    }

    private long product(Ctx c, String name, String price, String cost) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"TL-%s","name":"%s","unitPrice":%s,"purchasePrice":%s,"minStock":0,"unitsPerBox":1,
                 "saleType":"WEIGHT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name, price, cost)))
                .get("id").asLong();
    }

    private void entry(Ctx c, long productId, String kg) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}""".formatted(productId, c.warehouseId(), kg))
                .andExpect(status().isOk());
    }

    private BigDecimal stock(Ctx c, long productId) throws Exception {
        for (JsonNode s : getJson("/api/inventory/stocks?warehouseId=" + c.warehouseId(), c)) {
            if (s.get("productId").asLong() == productId) return money(s, "quantity");
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
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, new Ctx(token, companyId, 0));
            if (!warehouses.isEmpty()) return new Ctx(token, companyId, warehouses.get(0).get("id").asLong());
        }
        throw new IllegalStateException("No company with a sales warehouse in the seed.");
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

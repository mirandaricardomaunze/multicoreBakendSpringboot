package mz.multicore.erp.modules.productionplan;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bakery day plan end to end over HTTP ({@code docs/PLANO_PRODUCAO_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:productionplan;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionPlanHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void planRequirementsBatchesAndClosingOfTheDay() throws Exception {
        Ctx c = context();
        long flour = product(c, "Farinha de trigo (teste)", "0", "50", false);
        long yeast = product(c, "Fermento (teste)", "0", "400", false);
        long bread = product(c, "Pão de forma (teste)", "15", "0", true);
        entry(c, flour, "4");
        entry(c, yeast, "1");
        long bomId = json(send(post("/api/manufacturing/boms"), c, """
                {"bomCode":"PAO-%s","name":"Pão de forma","finishedProductId":%d,"baseQuantity":100,"bomType":"STANDARD",
                 "estimatedLaborCost":100,"estimatedOverheadCost":0,
                 "items":[{"rawMaterialProductId":%d,"requiredQuantity":5,"scrapPercentage":0},
                          {"rawMaterialProductId":%d,"requiredQuantity":0.5,"scrapPercentage":0}]}"""
                .formatted(UUID.randomUUID().toString().substring(0, 6), bread, flour, yeast))).get("id").asLong();

        LocalDate today = LocalDate.now();
        JsonNode plan = json(send(put("/api/production-plans"), c, """
                {"companyId":%s,"warehouseId":%d,"planDate":"%s","lines":[{"bomId":%d,"plannedQty":100}]}"""
                .formatted(c.companyId(), c.warehouseId(), today, bomId)));
        long planId = plan.get("id").asLong();
        long lineId = plan.get("lines").get(0).get("id").asLong();

        JsonNode needs = getJson("/api/production-plans/" + planId + "/requirements?companyId=" + c.companyId(), c);
        JsonNode flourNeed = find(needs, flour);
        assertThat(money(flourNeed, "required")).isEqualByComparingTo("5");
        assertThat(money(flourNeed, "shortage")).as("4 kg in stock for 5 kg needed").isEqualByComparingTo("1");

        send(post("/api/production-plans/runs"), c, run(c, lineId, "100", today.plusDays(2))).andExpect(status().isBadRequest());
        String key = "run-" + UUID.randomUUID();
        String body = """
                {"companyId":%s,"planLineId":%d,"quantity":60,"expirationDate":"%s","requestKey":"%s"}"""
                .formatted(c.companyId(), lineId, today.plusDays(2), key);
        JsonNode made = json(send(post("/api/production-plans/runs"), c, body));
        // 3 kg flour x 50 + 0.3 kg yeast x 400 + labour 100 x 60/100 = 150 + 120 + 60 = 330 -> 5.50 per bread.
        assertThat(money(made, "totalCost")).isEqualByComparingTo("330");
        assertThat(money(made, "unitCost")).isEqualByComparingTo("5.5");
        assertThat(made.get("batchNumber").asText()).startsWith("PD");
        assertThat(json(send(post("/api/production-plans/runs"), c, body)).get("id").asLong()).isEqualTo(made.get("id").asLong());
        assertThat(stock(c, flour)).isEqualByComparingTo("1");
        assertThat(stock(c, bread)).isEqualByComparingTo("60");

        send(post("/api/production-plans/runs"), c, """
                {"companyId":%s,"planLineId":%d,"quantity":10,"requestKey":"run-%s"}""".formatted(c.companyId(), lineId, UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        JsonNode closed = json(send(post("/api/production-plans/" + planId + "/close"), c, """
                {"companyId":%s,"leftovers":[{"planLineId":%d,"quantity":8,"writeOff":true}]}""".formatted(c.companyId(), lineId)));
        assertThat(closed.get("status").asText()).isEqualTo("CLOSED");
        assertThat(money(closed.get("lines").get(0), "leftoverQty")).isEqualByComparingTo("8");
        assertThat(stock(c, bread)).isEqualByComparingTo("52");
        send(put("/api/production-plans"), c, """
                {"companyId":%s,"warehouseId":%d,"planDate":"%s","lines":[{"bomId":%d,"plannedQty":120}]}"""
                .formatted(c.companyId(), c.warehouseId(), today, bomId)).andExpect(status().isBadRequest());

        JsonNode suggestions = getJson("/api/production-plans/suggestions?companyId=" + c.companyId() + "&date=" + today, c);
        assertThat(suggestions.toString()).contains("Pão de forma (teste)");
    }

    private String run(Ctx c, long lineId, String qty, LocalDate expiry) {
        return """
                {"companyId":%s,"planLineId":%d,"quantity":%s,"expirationDate":"%s","requestKey":"run-%s"}"""
                .formatted(c.companyId(), lineId, qty, expiry, UUID.randomUUID());
    }

    private JsonNode find(JsonNode list, long productId) {
        for (JsonNode n : list) if (n.get("productId").asLong() == productId) return n;
        throw new IllegalStateException("Product " + productId + " not in " + list);
    }

    private long product(Ctx c, String name, String price, String cost, boolean perishable) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"PD-%s","name":"%s","unitPrice":%s,"purchasePrice":%s,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true,"tracksExpiry":%s}"""
                .formatted(UUID.randomUUID().toString().substring(0, 8), name, price, cost, perishable))).get("id").asLong();
    }

    private void entry(Ctx c, long productId, String qty) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}""".formatted(productId, c.warehouseId(), qty))
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

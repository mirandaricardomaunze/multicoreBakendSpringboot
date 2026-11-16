package mz.multicore.erp.modules.bulkstock;

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

/**
 * Bulk stock measured on the pile, end to end over HTTP ({@code docs/STOCK_GRANEL_SPEC.md}): tolerance,
 * manager approval, tonne conversion through product units, and sold material awaiting pickup.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:bulk-stock;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BulkStockHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void sandPileWithinToleranceAboveToleranceAndWithPendingPickups() throws Exception {
        Ctx c = context();
        long sand = createProduct(c, "Areia grossa (teste granel)");
        send(put("/api/units/products/base"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"M3"}""".formatted(c.companyId(), sand)).andExpect(status().isOk());
        addStock(c, sand, "100");

        // Not yet a bulk product: measuring is refused.
        measure(c, sand, "\"method\":\"DIRECT\",\"quantity\":100", "/preview").andExpect(status().isBadRequest());
        send(put("/api/bulk-stock/settings"), c, """
                {"companyId":%s,"productId":%d,"tolerancePercent":5}""".formatted(c.companyId(), sand)).andExpect(status().isOk());

        JsonNode same = json(measure(c, sand, "\"method\":\"BOX\",\"dimension1\":10,\"dimension2\":5,\"dimension3\":2", ""));
        assertThat(same.get("status").asText()).isEqualTo("NO_DIFFERENCE");

        JsonNode big = json(measure(c, sand, "\"method\":\"HEAP\",\"dimension1\":10,\"dimension2\":5,\"dimension3\":2", ""));
        assertThat(big.get("status").asText()).isEqualTo("PENDING_APPROVAL");
        assertThat(stock(c, sand)).as("above tolerance waits").isEqualByComparingTo("100");
        send(post("/api/bulk-stock/measurements/" + big.get("id").asLong() + "/approve"), c,
                "{\"companyId\":" + c.companyId() + "}").andExpect(status().isOk());
        assertThat(stock(c, sand)).isEqualByComparingTo("50");
        send(post("/api/bulk-stock/measurements/" + big.get("id").asLong() + "/approve"), c,
                "{\"companyId\":" + c.companyId() + "}").andExpect(status().isBadRequest());

        JsonNode small = json(measure(c, sand, "\"method\":\"DIRECT\",\"quantity\":48", ""));
        assertThat(small.get("status").asText()).isEqualTo("APPLIED");
        assertThat(stock(c, sand)).isEqualByComparingTo("48");

        // Sell 10 m3 that the client collects later: the pile still holds them.
        JsonNode invoice = json(send(post("/api/comercial/invoices"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,
                 "lines":[{"productId":%d,"quantity":10,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), sand)));
        send(post("/api/pickups/accounts"), c, """
                {"companyId":%s,"invoiceId":%d}""".formatted(c.companyId(), invoice.get("id").asLong())).andExpect(status().isOk());
        JsonNode withPickup = json(measure(c, sand, "\"method\":\"DIRECT\",\"quantity\":48", "/preview"));
        assertThat(new BigDecimal(withPickup.get("pendingPickup").asText())).isEqualByComparingTo("10");
        assertThat(new BigDecimal(withPickup.get("difference").asText())).isEqualByComparingTo("0");
        measure(c, sand, "\"method\":\"DIRECT\",\"quantity\":5", "").andExpect(status().isBadRequest());
    }

    @Test
    void tonneBasedProductConvertsThePileVolume() throws Exception {
        Ctx c = context();
        long stone = createProduct(c, "Brita 3/4 (teste granel)");
        send(put("/api/units/products/base"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"TON"}""".formatted(c.companyId(), stone)).andExpect(status().isOk());
        send(put("/api/bulk-stock/settings"), c, """
                {"companyId":%s,"productId":%d,"tolerancePercent":3}""".formatted(c.companyId(), stone)).andExpect(status().isOk());
        // Without a cubic-metre factor the volume cannot become tonnes.
        measure(c, stone, "\"method\":\"BOX\",\"dimension1\":1,\"dimension2\":1,\"dimension3\":1", "/preview")
                .andExpect(status().isBadRequest());
        send(put("/api/units/products"), c, """
                {"companyId":%s,"productId":%d,"unitCode":"M3","factorToBase":1.6,
                 "sellable":true,"purchasable":true,"defaultSale":false,"defaultPurchase":false}"""
                .formatted(c.companyId(), stone)).andExpect(status().isOk());
        JsonNode preview = json(measure(c, stone, "\"method\":\"BOX\",\"dimension1\":2,\"dimension2\":1,\"dimension3\":1", "/preview"));
        assertThat(new BigDecimal(preview.get("volumeM3").asText())).isEqualByComparingTo("2");
        assertThat(new BigDecimal(preview.get("measuredQuantity").asText())).isEqualByComparingTo("3.2");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private ResultActions measure(Ctx c, long productId, String fields, String suffix) throws Exception {
        return send(post("/api/bulk-stock/measurements" + suffix), c, """
                {"companyId":%s,"productId":%d,"warehouseId":%d,%s}""".formatted(c.companyId(), productId, c.warehouseId(), fields));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long createProduct(Ctx c, String name) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"GR-%s","name":"%s","unitPrice":1800,"purchasePrice":1,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name)))
                .get("id").asLong();
    }

    private void addStock(Ctx c, long productId, String quantity) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY"}"""
                .formatted(productId, c.warehouseId(), quantity)).andExpect(status().isOk());
    }

    private BigDecimal stock(Ctx c, long productId) throws Exception {
        for (JsonNode s : getJson("/api/inventory/stocks?companyId=" + c.companyId() + "&warehouseId=" + c.warehouseId(), c)) {
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
            Ctx probe = new Ctx(token, companyId, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/sales?companyId=" + companyId, probe);
            Long namedClient = null;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if (!"000000000".equals(client.path("taxId").asText())) { namedClient = client.get("id").asLong(); break; }
            }
            if (warehouses.isEmpty() || namedClient == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), namedClient);
        }
        throw new IllegalStateException("No company with a sales warehouse and a named client in the seed.");
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

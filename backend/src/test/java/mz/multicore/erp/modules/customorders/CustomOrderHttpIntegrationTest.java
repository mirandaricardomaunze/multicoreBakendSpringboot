package mz.multicore.erp.modules.customorders;

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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Custom orders end to end ({@code docs/ENCOMENDAS_PERSONALIZADAS_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:customorders;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomOrderHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId) {}

    @Test
    void birthdayCakeIsScheduledPersonalisedSentToProductionAndPrinted() throws Exception {
        Ctx c = context();
        long flour = product(c, "Farinha (teste bolo)", "0");
        long cake = product(c, "Bolo de aniversário (teste)", "2500");
        json(send(post("/api/manufacturing/boms"), c, """
                {"bomCode":"BOLO-%s","name":"Bolo de aniversário","finishedProductId":%d,"baseQuantity":1,"bomType":"STANDARD",
                 "items":[{"rawMaterialProductId":%d,"requiredQuantity":0.8,"scrapPercentage":0}]}"""
                .formatted(UUID.randomUUID().toString().substring(0, 6), cake, flour)));
        long orderId = json(send(post("/api/comercial/orders"), c, """
                {"clientId":%d,"companyId":%s,"warehouseId":%d,"lines":[{"productId":%d,"quantity":2,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(c.clientId(), c.companyId(), c.warehouseId(), cake))).get("id").asLong();
        LocalDate saturday = LocalDate.now().plusDays(3);

        send(put("/api/custom-orders/" + orderId), c, body(c, saturday, "DELIVERY", null, png())).andExpect(status().isBadRequest());
        send(put("/api/custom-orders/" + orderId), c, body(c, saturday, "PICKUP", null, Base64.getEncoder().encodeToString("abc".getBytes())))
                .andExpect(status().isBadRequest());
        JsonNode saved = json(send(put("/api/custom-orders/" + orderId), c, body(c, saturday, "PICKUP", "15:00", png())));
        assertThat(saved.get("hasImage").asBoolean()).isTrue();
        assertThat(saved.get("personalization").asText()).contains("Parabéns Ana");

        JsonNode agenda = getJson("/api/custom-orders/agenda?companyId=" + c.companyId() + "&from=" + LocalDate.now() + "&to=" + saturday.plusDays(1), c);
        JsonNode entry = null;
        for (JsonNode n : agenda) if (n.get("orderId").asLong() == orderId) entry = n;
        assertThat(entry).isNotNull();
        assertThat(entry.get("deliveryTime").asText()).startsWith("15:00");
        assertThat(entry.get("stage").asText()).isEqualTo("TO_PRODUCE");

        JsonNode sent = json(send(post("/api/custom-orders/" + orderId + "/send-to-plan?companyId=" + c.companyId()), c, "{}"));
        assertThat(sent.get("planned").toString()).contains("Bolo de aniversário (teste)");
        send(post("/api/custom-orders/" + orderId + "/send-to-plan?companyId=" + c.companyId()), c, "{}").andExpect(status().isBadRequest());
        JsonNode plan = getJson("/api/production-plans/day?companyId=" + c.companyId() + "&warehouseId=" + c.warehouseId() + "&date=" + saturday, c);
        assertThat(new BigDecimal(plan.get("lines").get(0).get("plannedQty").asText())).isEqualByComparingTo("2");

        JsonNode ready = json(send(post("/api/custom-orders/" + orderId + "/stage"), c,
                "{\"companyId\":%s,\"stage\":\"READY\"}".formatted(c.companyId())));
        assertThat(ready.get("stage").asText()).isEqualTo("READY");

        byte[] pdf = mockMvc.perform(auth(get("/api/custom-orders/" + orderId + "/sheet?companyId=" + c.companyId()), c))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    private String body(Ctx c, LocalDate date, String mode, String time, String image) {
        return """
                {"companyId":%s,"deliveryDate":"%s",%s"mode":"%s","contactPhone":"84 000 0000",
                 "personalization":"Parabéns Ana! 30 anos, cobertura de chocolate","imageBase64":"%s"}"""
                .formatted(c.companyId(), date, time == null ? "" : "\"deliveryTime\":\"" + time + "\",", mode, image);
    }

    private static String png() throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private long product(Ctx c, String name, String price) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"EN-%s","name":"%s","unitPrice":%s,"purchasePrice":1,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":false}""".formatted(UUID.randomUUID().toString().substring(0, 8), name, price)))
                .get("id").asLong();
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
            Long named = null;
            for (JsonNode client : getJson("/api/comercial/clients", probe)) {
                if (!"000000000".equals(client.path("taxId").asText()) && named == null) named = client.get("id").asLong();
            }
            if (warehouses.isEmpty() || named == null) continue;
            return new Ctx(token, companyId, warehouses.get(0).get("id").asLong(), named);
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

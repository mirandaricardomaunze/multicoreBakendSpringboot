package mz.multicore.erp.modules.expiry;

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

/** Expiry board, markdown rules and write-off end to end ({@code docs/VALIDADES_REMARCACAO_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:expiry;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpiryHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired private mz.multicore.erp.modules.inventory.repository.ProductBatchRepository batchRepository;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void lotsAreShownMarkedDownByTheRulesAndWrittenOffWhenExpired() throws Exception {
        Ctx c = context();
        long meat = product(c, "Carne picada (teste validade)");
        String freshLot = "L-" + UUID.randomUUID().toString().substring(0, 6);
        String oldLot = "L-" + UUID.randomUUID().toString().substring(0, 6);
        entry(c, meat, "12", freshLot, LocalDate.now().plusDays(1));
        entry(c, meat, "3", oldLot, LocalDate.now().plusDays(5));
        // The lot was fine when it arrived and expired on the shelf.
        batchRepository.findAll().stream().filter(b -> oldLot.equals(b.getBatchNumber())).forEach(b -> {
            b.setExpirationDate(LocalDate.now().minusDays(1));
            batchRepository.save(b);
        });

        JsonNode board = getJson("/api/expiry/board?companyId=" + c.companyId() + "&days=14", c);
        JsonNode fresh = lot(board, freshLot);
        JsonNode old = lot(board, oldLot);
        assertThat(fresh.get("stage").asText()).isEqualTo("SOON");
        assertThat(old.get("stage").asText()).isEqualTo("EXPIRED");
        assertThat(new BigDecimal(old.get("costValue").asText())).isEqualByComparingTo("300");

        json(send(put("/api/expiry/rules"), c, """
                {"companyId":%s,"daysBefore":2,"percent":30,"active":true}""".formatted(c.companyId())));
        JsonNode applied = json(send(post("/api/expiry/markdowns/apply-rules?companyId=" + c.companyId()), c, "{}"));
        assertThat(applied.get("created").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode again = json(send(post("/api/expiry/markdowns/apply-rules?companyId=" + c.companyId()), c, "{}"));
        assertThat(again.get("products").toString()).as("a second run does not mark the same product down twice")
                .doesNotContain("Carne picada (teste validade)");

        JsonNode after = lot(getJson("/api/expiry/board?companyId=" + c.companyId() + "&days=14", c), freshLot);
        assertThat(new BigDecimal(after.get("markdownPct").asText())).isEqualByComparingTo("30");
        assertThat(new BigDecimal(after.get("suggestedPct").asText())).isEqualByComparingTo("30");

        send(post("/api/expiry/markdowns"), c, """
                {"companyId":%s,"batchId":%d,"percent":20}""".formatted(c.companyId(), fresh.get("batchId").asLong()))
                .andExpect(status().isBadRequest());
        send(post("/api/expiry/write-offs"), c, """
                {"companyId":%s,"batchId":%d,"requestKey":"wo-%s"}""".formatted(c.companyId(), fresh.get("batchId").asLong(), UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        JsonNode waste = json(send(post("/api/expiry/write-offs"), c, """
                {"companyId":%s,"batchId":%d,"notes":"Cheiro alterado","requestKey":"wo-%s"}"""
                .formatted(c.companyId(), old.get("batchId").asLong(), UUID.randomUUID())));
        assertThat(waste.get("reason").asText()).isEqualTo("EXPIRED");
    }

    private JsonNode lot(JsonNode board, String batchNumber) {
        for (JsonNode lot : board.get("lots")) if (batchNumber.equals(lot.get("batchNumber").asText())) return lot;
        throw new IllegalStateException("Lot " + batchNumber + " not on the board: " + board);
    }

    private long product(Ctx c, String name) throws Exception {
        return json(send(post("/api/comercial/products"), c, """
                {"sku":"VL-%s","name":"%s","unitPrice":450,"purchasePrice":100,"minStock":0,"unitsPerBox":1,
                 "saleType":"WEIGHT","stockTracked":true,"tracksExpiry":true}""".formatted(UUID.randomUUID().toString().substring(0, 8), name)))
                .get("id").asLong();
    }

    private void entry(Ctx c, long productId, String qty, String batch, LocalDate expiry) throws Exception {
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":%s,"movementType":"ENTRY","batchNumber":"%s","expirationDate":"%s"}"""
                .formatted(productId, c.warehouseId(), qty, batch, expiry)).andExpect(status().isOk());
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

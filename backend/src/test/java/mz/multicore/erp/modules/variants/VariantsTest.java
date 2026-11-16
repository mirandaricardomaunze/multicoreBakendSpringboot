package mz.multicore.erp.modules.variants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.variants.service.VariantRules;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Product variants: rules and HTTP end to end ({@code docs/VARIANTES_ARTIGO_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:variants;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VariantsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId) {}

    @Test
    void combinationsCodesAndNames() {
        assertThat(VariantRules.clean(Arrays.asList(" M ", "m", "", null, "L"))).containsExactly("M", "L");
        List<VariantRules.Combination> c = VariantRules.combinations(List.of("M", "L"), List.of("Azul", "Preto"));
        assertThat(c).hasSize(4);
        assertThat(VariantRules.combinations(List.of("8mm", "10mm"), List.of())).hasSize(2);
        assertThat(VariantRules.sku("polo", new VariantRules.Combination("M", "Azul Marinho"))).isEqualTo("POLO-M-AZULMARINHO");
        assertThat(VariantRules.sku("var", new VariantRules.Combination("Ø 12 mm", null))).isEqualTo("VAR-12MM");
        assertThat(VariantRules.name("Camisa Polo", c.get(1))).isEqualTo("Camisa Polo - M / Preto");
    }

    @Test
    void familyCreatesSellableVariantsAndGrowsWithNewValues() throws Exception {
        Ctx c = context();
        long template = json(send(post("/api/comercial/products"), c, """
                {"sku":"TPL-%s","name":"Camisa Polo (modelo)","unitPrice":850,"purchasePrice":400,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8)))).get("id").asLong();
        String prefix = "P" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String body = """
                {"companyId":%s,"name":"Camisa Polo","skuPrefix":"%s","templateProductId":%d,"attribute1":"Tamanho",
                 "values1":["S","M","L"],"attribute2":"Cor","values2":["Azul","Preto"]}""".formatted(c.companyId(), prefix, template);
        JsonNode family = json(send(post("/api/variants/families"), c, body));
        assertThat(family.get("variants").size()).isEqualTo(6);
        assertThat(family.get("variants").get(0).get("sku").asText()).isEqualTo(prefix + "-S-AZUL");
        assertThat(new BigDecimal(family.get("variants").get(0).get("unitPrice").asText())).isEqualByComparingTo("850");
        send(post("/api/variants/families"), c, body).andExpect(status().isBadRequest());

        long variantProduct = family.get("variants").get(1).get("productId").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":12,"movementType":"ENTRY"}""".formatted(variantProduct, c.warehouseId()))
                .andExpect(status().isOk());

        long familyId = family.get("id").asLong();
        JsonNode grown = json(send(post("/api/variants/families/" + familyId + "/values"), c,
                "{\"companyId\":%s,\"values1\":[\"XL\"],\"values2\":[]}".formatted(c.companyId())));
        assertThat(grown.get("variants").size()).isEqualTo(8);
        send(post("/api/variants/families/" + familyId + "/values"), c,
                "{\"companyId\":%s,\"values1\":[\"m\"],\"values2\":[]}".formatted(c.companyId())).andExpect(status().isBadRequest());

        JsonNode priced = json(send(put("/api/variants/families/" + familyId + "/price"), c,
                "{\"companyId\":%s,\"unitPrice\":900}".formatted(c.companyId())));
        for (JsonNode v : priced.get("variants")) assertThat(new BigDecimal(v.get("unitPrice").asText())).isEqualByComparingTo("900");

        JsonNode listed = null;
        for (JsonNode f : getJson("/api/variants/families?companyId=" + c.companyId(), c)) if (f.get("id").asLong() == familyId) listed = f;
        assertThat(listed).isNotNull();
        assertThat(new BigDecimal(listed.get("totalStock").asText())).isEqualByComparingTo("12");
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

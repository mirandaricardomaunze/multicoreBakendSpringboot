package mz.multicore.erp.modules.projects;

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

/** Client projects end to end over HTTP ({@code docs/OBRAS_CLIENTE_SPEC.md}). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:projects;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerProjectHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Ctx(String token, String companyId, long warehouseId, long clientId, long walkInId) {}

    @Test
    void contractorGetsOneStatementPerSiteAndForeignOrdersAreRefused() throws Exception {
        Ctx c = context();
        JsonNode project = json(send(put("/api/projects"), c, """
                {"companyId":%s,"clientId":%d,"name":"Moradia Triunfo","address":"Bairro Triunfo, Maputo","active":true}"""
                .formatted(c.companyId(), c.clientId())));
        assertThat(project.get("code").asText()).startsWith("OB-");
        long projectId = project.get("id").asLong();

        if (c.walkInId() > 0) {
            send(put("/api/projects"), c, """
                    {"companyId":%s,"clientId":%d,"name":"Indevida","active":true}""".formatted(c.companyId(), c.walkInId()))
                    .andExpect(status().isBadRequest());
        }

        long productId = json(send(post("/api/comercial/products"), c, """
                {"sku":"OB-%s","name":"Bloco 15 (teste obra)","unitPrice":45,"purchasePrice":1,"minStock":0,"unitsPerBox":1,
                 "saleType":"UNIT","stockTracked":true}""".formatted(UUID.randomUUID().toString().substring(0, 8)))).get("id").asLong();
        send(post("/api/inventory/movements"), c, """
                {"productId":%d,"warehouseId":%d,"quantity":1000,"movementType":"ENTRY"}""".formatted(productId, c.warehouseId()))
                .andExpect(status().isOk());

        long orderId = order(c, c.clientId(), productId);
        JsonNode linked = json(send(put("/api/projects/" + projectId + "/orders/" + orderId + "?companyId=" + c.companyId()), c, ""));
        assertThat(linked.get("orderCount").asInt()).isEqualTo(1);

        JsonNode statement = getJson("/api/projects/" + projectId + "/statement?companyId=" + c.companyId(), c);
        assertThat(statement.get("lines").size()).isEqualTo(1);
        assertThat(new BigDecimal(statement.get("orderedTotal").asText())).isPositive();
        JsonNode ofOrder = getJson("/api/projects/orders/" + orderId + "?companyId=" + c.companyId(), c);
        assertThat(ofOrder.get("id").asLong()).isEqualTo(projectId);

        long walkInOrder = order(c, null, productId);
        send(put("/api/projects/" + projectId + "/orders/" + walkInOrder + "?companyId=" + c.companyId()), c, "")
                .andExpect(status().isBadRequest());

        mockMvc.perform(auth(delete("/api/projects/orders/" + orderId + "?companyId=" + c.companyId()), c))
                .andExpect(status().isNoContent());
        assertThat(getJson("/api/projects/" + projectId + "/statement?companyId=" + c.companyId(), c).get("lines").size()).isZero();
    }

    private long order(Ctx c, Long clientId, long productId) throws Exception {
        String client = clientId == null ? "" : "\"clientId\":" + clientId + ",";
        return json(send(post("/api/comercial/orders"), c, """
                {%s"companyId":%s,"warehouseId":%d,"lines":[{"productId":%d,"quantity":100,"taxRate":0.16,"discountPercentage":0}]}"""
                .formatted(client, c.companyId(), c.warehouseId(), productId))).get("id").asLong();
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
        return mockMvc.perform(auth(req, c).contentType(MediaType.APPLICATION_JSON).content(body.isEmpty() ? "{}" : body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}

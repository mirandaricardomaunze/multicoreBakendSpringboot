package mz.multicore.erp.modules.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.activity.service.ActivityCapabilityPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SA-C01 end to end: the server refuses new sector work outside the profile and seeds starting categories. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:activity-capability;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ActivityCapabilityHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping mappings;

    private static final String DENIED = "não está activo no perfil de actividade";

    @Test
    @Order(1)
    @DisplayName("Sem perfil passa; com perfil sem a capacidade recusa; concluir continua permitido")
    void profileGatesNewSectorWork() throws Exception {
        String[] ctx = login();

        assertThat(body(send(post("/api/weighbridge/tickets"), ctx, "{}"))).doesNotContain(DENIED);

        saveProfiles(ctx, "SERVICES");
        MvcResult refused = send(post("/api/weighbridge/tickets"), ctx, "{}");
        assertThat(refused.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(refused)).contains("Báscula de camião", DENIED);

        assertThat(body(send(post("/api/weighbridge/tickets/999999/second"), ctx, "{}"))).doesNotContain(DENIED);
        assertThat(body(mockMvc.perform(auth(get("/api/advances"), ctx)).andReturn())).doesNotContain(DENIED);

        saveProfiles(ctx, "SERVICES", "BUILDING_MATERIALS");
        assertThat(body(send(post("/api/weighbridge/tickets"), ctx, "{}"))).doesNotContain(DENIED);
    }

    @Test
    @Order(2)
    @DisplayName("Escolher um perfil cria as categorias do sector uma só vez")
    void newProfileSeedsCategoriesOnce() throws Exception {
        String[] ctx = login();
        saveProfiles(ctx, "BUTCHER");
        List<String> codes = categoryCodes(ctx);
        assertThat(codes).contains("SA-CARNE-VACA", "SA-ENCHIDOS", "SA-MIUDEZAS");

        saveProfiles(ctx, "BUTCHER", "GENERAL_RETAIL");
        saveProfiles(ctx, "BUTCHER", "GENERAL_RETAIL");
        List<String> again = categoryCodes(ctx);
        assertThat(again).contains("SA-GERAL");
        assertThat(again.stream().filter("SA-CARNE-VACA"::equals).count()).isEqualTo(1);

        saveProfiles(ctx, "SERVICES");
        assertThat(categoryCodes(ctx)).contains("SA-CARNE-VACA", "SA-SERVICOS");
    }

    @Test
    @DisplayName("Cada regra da política aponta para um endpoint que existe")
    void everyPolicyRuleMatchesARealEndpoint() {
        List<String[]> endpoints = new ArrayList<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
            for (String path : mapping.getPatternValues()) {
                for (RequestMethod m : methods) endpoints.add(new String[]{m.name(), path.replaceAll("\\{[^}]+}", "1")});
            }
        });
        for (String[] probe : List.of(
                new String[]{"POST", "/api/advances"}, new String[]{"PUT", "/api/custom-orders/1"},
                new String[]{"POST", "/api/consignments"}, new String[]{"PUT", "/api/returnables/items"},
                new String[]{"POST", "/api/variants/families"}, new String[]{"POST", "/api/variants/families/1/values"},
                new String[]{"POST", "/api/weighbridge/tickets"}, new String[]{"PUT", "/api/butchery/templates"},
                new String[]{"POST", "/api/butchery/breakdowns"}, new String[]{"PUT", "/api/expiry/rules"},
                new String[]{"POST", "/api/expiry/markdowns"}, new String[]{"POST", "/api/expiry/markdowns/apply-rules"},
                new String[]{"PUT", "/api/production-plans"}, new String[]{"POST", "/api/production-plans/runs"},
                new String[]{"PUT", "/api/projects"}, new String[]{"PUT", "/api/projects/1/orders/1"},
                new String[]{"PUT", "/api/pricing/lists"}, new String[]{"PUT", "/api/pricing/items"},
                new String[]{"PUT", "/api/pricing/clients"}, new String[]{"PUT", "/api/bulk-stock/settings"},
                new String[]{"POST", "/api/bulk-stock/measurements"}, new String[]{"POST", "/api/bulk-stock/measurements/preview"},
                new String[]{"PUT", "/api/units/products"}, new String[]{"POST", "/api/pickups/accounts"})) {
            assertThat(ActivityCapabilityPolicy.required(probe[0], probe[1])).as(probe[0] + " " + probe[1]).isPresent();
            assertThat(endpoints).as("endpoint real para " + probe[0] + " " + probe[1])
                    .anyMatch(e -> e[0].equals(probe[0]) && e[1].equals(probe[1]));
        }
    }

    private void saveProfiles(String[] ctx, String... profiles) throws Exception {
        String body = om.writeValueAsString(java.util.Map.of("companyId", Long.parseLong(ctx[1]), "profiles", List.of(profiles)));
        MvcResult result = send(put("/api/activity/company"), ctx, body);
        assertThat(result.getResponse().getStatus()).as(body(result)).isEqualTo(200);
    }

    private List<String> categoryCodes(String[] ctx) throws Exception {
        JsonNode list = om.readTree(body(mockMvc.perform(auth(get("/api/product-categories"), ctx))
                .andExpect(status().isOk()).andReturn()));
        List<String> codes = new ArrayList<>();
        list.forEach(c -> codes.add(c.get("code").asText()));
        return codes;
    }

    private String[] login() throws Exception {
        JsonNode login = om.readTree(body(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"password\"}")).andExpect(status().isOk()).andReturn()));
        return new String[]{login.get("token").asText(), login.get("companies").get(0).get("id").asText()};
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String[] ctx) {
        return req.header("Authorization", "Bearer " + ctx[0]).header("X-Company-Id", ctx[1]);
    }

    private MvcResult send(MockHttpServletRequestBuilder req, String[] ctx, String body) throws Exception {
        return mockMvc.perform(auth(req, ctx).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}

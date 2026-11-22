package mz.multicore.erp.modules.accounting;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness CC-09 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3): os endpoints de centros de custo e do relatório de
 * resultados por centro; gerir só ADMIN/MANAGER, sempre na empresa activa.
 * Utilizadores semeados: ana=ADMIN (PT+MZ), joao=MANAGER (PT), maria=EMPLOYEE (PT). Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:cost-center-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CostCenterHttpIntegrationTest {

    private static final String CENTERS = "/api/accounting/cost-centers";
    private static final String RESULT = "/api/accounting/cost-center-result";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Session(String token, String companyId) {}

    @Test
    void cc09_gestorCriaRenomeiaMudaOPorOmissaoEApagaCentrosManuais() throws Exception {
        Session manager = login("joao", null);
        String code = code();

        JsonNode created = json(send(post(CENTERS), manager, center(code, "Frota", null)));
        long id = created.get("id").asLong();
        assertThat(created.get("type").asText()).isEqualTo("MANUAL");
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(created.get("defaultCenter").asBoolean()).isFalse();
        assertThat(codes(manager)).contains(code, "ADM");

        send(post(CENTERS), manager, center(code, "Duplicado", null)).andExpect(status().isBadRequest());
        send(post(CENTERS), manager, center("", "Sem código", null)).andExpect(status().isBadRequest());

        JsonNode renamed = json(send(put(CENTERS + "/" + id), manager, center(code, "Frota e logística", null)));
        assertThat(renamed.get("name").asText()).isEqualTo("Frota e logística");

        long admId = centerId(manager, "ADM");
        JsonNode promoted = json(send(put(CENTERS + "/" + id + "/default"), manager, ""));
        assertThat(promoted.get("defaultCenter").asBoolean()).isTrue();
        send(delete(CENTERS + "/" + id), manager, "").andExpect(status().isBadRequest());
        json(send(put(CENTERS + "/" + admId + "/default"), manager, ""));

        send(delete(CENTERS + "/" + id), manager, "").andExpect(status().isNoContent());
        assertThat(codes(manager)).doesNotContain(code);
    }

    @Test
    void cc09_relatorioDeResultadosPorCentroBateComADemonstracao() throws Exception {
        Session manager = login("joao", null);
        String period = "?from=2000-01-01&to=" + LocalDate.now().plusDays(1);

        JsonNode report = getJson(RESULT + period, manager);
        JsonNode statement = getJson("/api/accounting/income-statement" + period, manager);

        assertThat(report.get("centers").isArray()).isTrue();
        assertThat(money(report, "netResult")).isEqualByComparingTo(money(statement, "netResult"));
        assertThat(money(report, "totalRevenue")).isEqualByComparingTo(money(statement, "totalRevenue"));
        assertThat(money(report, "totalExpense")).isEqualByComparingTo(money(statement, "totalExpense"));

        send(get(RESULT + "?from=2026-02-01&to=2026-01-01"), manager, "").andExpect(status().isBadRequest());
    }

    @Test
    void cc09_soPerfisDeGestaoESoEmpresaActiva() throws Exception {
        Session manager = login("joao", null);
        String code = code();
        long id = json(send(post(CENTERS), manager, center(code, "Produção", null))).get("id").asLong();

        Session employee = login("maria", manager.companyId());
        mockMvc.perform(auth(get(CENTERS), employee)).andExpect(status().isForbidden());
        send(post(CENTERS), employee, center(code(), "Intruso", null)).andExpect(status().isForbidden());
        send(put(CENTERS + "/" + id), employee, center(code, "Intruso", null)).andExpect(status().isForbidden());
        send(put(CENTERS + "/" + id + "/default"), employee, "").andExpect(status().isForbidden());
        send(delete(CENTERS + "/" + id), employee, "").andExpect(status().isForbidden());
        mockMvc.perform(auth(get(RESULT + "?from=2026-01-01&to=2026-01-31"), employee))
                .andExpect(status().isForbidden());

        Session adminElsewhere = login("ana", otherCompanyThan(manager.companyId()));
        assertThat(codes(adminElsewhere)).doesNotContain(code);
        send(put(CENTERS + "/" + id), adminElsewhere, center(code, "Outra empresa", null))
                .andExpect(status().isBadRequest());
        send(put(CENTERS + "/" + id + "/default"), adminElsewhere, "").andExpect(status().isBadRequest());
        send(delete(CENTERS + "/" + id), adminElsewhere, "").andExpect(status().isBadRequest());
        assertThat(codes(manager)).contains(code);
    }

    // --- cenário ---

    private static String code() {
        return "T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private static String center(String code, String name, Boolean active) {
        return """
                {"code":"%s","name":"%s","active":%s}""".formatted(code, name, active);
    }

    private List<String> codes(Session s) throws Exception {
        return getJson(CENTERS, s).findValuesAsText("code");
    }

    private long centerId(Session s, String code) throws Exception {
        for (JsonNode center : getJson(CENTERS, s)) {
            if (code.equals(center.get("code").asText())) return center.get("id").asLong();
        }
        throw new AssertionError("Centro " + code + " não encontrado");
    }

    private String otherCompanyThan(String companyId) throws Exception {
        for (JsonNode company : loginJson("ana").get("companies")) {
            if (!company.get("id").asText().equals(companyId)) return company.get("id").asText();
        }
        throw new AssertionError("ana devia ter uma segunda empresa");
    }

    // --- HTTP ---

    private Session login(String username, String companyId) throws Exception {
        JsonNode login = loginJson(username);
        String active = companyId != null ? companyId : login.get("companies").get(0).get("id").asText();
        return new Session(login.get("token").asText(), active);
    }

    private JsonNode loginJson(String username) throws Exception {
        return read(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static BigDecimal money(JsonNode node, String field) {
        return new BigDecimal(node.get(field).asText());
    }

    private JsonNode json(ResultActions result) throws Exception {
        return read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, Session s) {
        return req.header("Authorization", "Bearer " + s.token()).header("X-Company-Id", s.companyId());
    }

    private JsonNode getJson(String path, Session s) throws Exception {
        return read(mockMvc.perform(auth(get(path), s)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private ResultActions send(MockHttpServletRequestBuilder req, Session s, String body) throws Exception {
        return mockMvc.perform(auth(req, s).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode read(String body) {
        try {
            return om.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException(body, e);
        }
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness OR-07 (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §4.1, §4.2): os endpoints do orçamento, do real vs orçado e
 * dos alertas; só ADMIN/MANAGER, sempre na empresa activa, com auditoria {@code BUDGET_CHANGE} ao gravar e copiar.
 * Utilizadores semeados: ana=ADMIN (PT+MZ), joao=MANAGER (PT), maria=EMPLOYEE (PT). Password "password".
 * Cada teste usa o seu próprio ano para não partilhar orçamento com os outros.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:budget-http;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BudgetHttpIntegrationTest {

    private static final String BUDGETS = "/api/accounting/budgets";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    private record Session(String token, String companyId) {}

    @Test
    void or07_gestorGravaConsultaECopiaOOrcamentoComAuditoria() throws Exception {
        Session manager = login("joao", null);
        long adm = admCenterId(manager);

        JsonNode empty = getJson(BUDGETS + "?year=2091", manager);
        assertThat(empty.get("lines").size()).isZero();

        JsonNode saved = json(send(put(BUDGETS), manager, """
                {"year":2091,"lines":[
                  {"accountCode":"6221","costCenterId":%d,"month":1,"amount":1500.00},
                  {"accountCode":"7101","costCenterId":%d,"month":1,"amount":9000.00}]}""".formatted(adm, adm)));
        assertThat(saved.get("lines").size()).isEqualTo(2);
        assertThat(money(saved, "totalExpenses")).isEqualByComparingTo("1500.00");
        assertThat(money(saved, "totalRevenues")).isEqualByComparingTo("9000.00");

        JsonNode read = getJson(BUDGETS + "?year=2091", manager);
        assertThat(read.get("id").asLong()).isEqualTo(saved.get("id").asLong());
        assertThat(read.findValuesAsText("accountCode")).containsExactly("6221", "7101");

        JsonNode copied = json(send(post(BUDGETS + "/copy-previous-year"), manager,
                "{\"year\":2091,\"adjustmentPercent\":10}"));
        assertThat(copied.get("lines").size()).isEqualTo(2);
        assertThat(money(copied, "totalExpenses")).isEqualByComparingTo("1500.00");

        JsonNode logs = getJson("/api/audit/logs", manager);
        long budgetChanges = 0;
        for (JsonNode log : logs) {
            if ("BUDGET_CHANGE".equals(log.get("action").asText())
                    && log.get("details").asText().contains("2091")) budgetChanges++;
        }
        assertThat(budgetChanges).isEqualTo(2);
        assertThat(logs.findValuesAsText("details")).anyMatch(d -> d.contains("2091") && d.contains("1500.00")
                && d.contains("9000.00"));
    }

    @Test
    void or07_regrasVioladasRespondem400ComMensagem() throws Exception {
        Session manager = login("joao", null);
        long adm = admCenterId(manager);

        String body = send(put(BUDGETS), manager, """
                {"year":2092,"lines":[{"accountCode":"1201","costCenterId":%d,"month":1,"amount":10}]}"""
                .formatted(adm)).andExpect(status().isBadRequest()).andReturn().getResponse()
                .getContentAsString();
        assertThat(body).contains("classes 6 e 7");
        send(put(BUDGETS), manager, """
                {"year":2092,"lines":[{"accountCode":"6221","costCenterId":%d,"month":13,"amount":10}]}"""
                .formatted(adm)).andExpect(status().isBadRequest());
        send(post(BUDGETS + "/copy-previous-year"), manager, "{\"year\":2092,\"adjustmentPercent\":-150}")
                .andExpect(status().isBadRequest());
        send(get(BUDGETS + "/variance?year=2092&month=0"), manager, "").andExpect(status().isBadRequest());
    }

    @Test
    void or07_realVsOrcadoEAlertasDaEmpresaActiva() throws Exception {
        Session manager = login("joao", null);
        long adm = admCenterId(manager);
        json(send(put(BUDGETS), manager, """
                {"year":2093,"lines":[{"accountCode":"6221","costCenterId":%d,"month":3,"amount":1000}]}"""
                .formatted(adm)));

        JsonNode report = getJson(BUDGETS + "/variance?year=2093&month=3&cumulative=false&costCenterId=" + adm,
                manager);
        assertThat(report.get("lines").size()).isEqualTo(1);
        JsonNode line = report.get("lines").get(0);
        assertThat(line.get("accountCode").asText()).isEqualTo("6221");
        assertThat(money(line, "budgeted")).isEqualByComparingTo("1000");
        assertThat(money(line, "actual")).isEqualByComparingTo("0");
        assertThat(money(line, "variance")).isEqualByComparingTo("-1000");
        assertThat(line.get("direction").asText()).isEqualTo("FAVORABLE");

        JsonNode cumulative = getJson(BUDGETS + "/variance?year=2093&month=12&cumulative=true", manager);
        assertThat(cumulative.get("cumulative").asBoolean()).isTrue();
        assertThat(cumulative.findValuesAsText("accountCode")).contains("6221");

        assertThat(getJson(BUDGETS + "/alerts?year=2093", manager).isArray()).isTrue();

        Session adminElsewhere = login("ana", otherCompanyThan(manager.companyId()));
        assertThat(getJson(BUDGETS + "?year=2093", adminElsewhere).get("lines").size()).isZero();
        assertThat(getJson(BUDGETS + "/variance?year=2093&month=3", adminElsewhere)
                .findValuesAsText("accountCode")).doesNotContain("6221");
        send(put(BUDGETS), adminElsewhere, """
                {"year":2093,"lines":[{"accountCode":"6221","costCenterId":%d,"month":3,"amount":5}]}"""
                .formatted(adm)).andExpect(status().isBadRequest());
        assertThat(money(getJson(BUDGETS + "?year=2093", manager), "totalExpenses")).isEqualByComparingTo("1000");
    }

    @Test
    void or07_soPerfisDeGestao() throws Exception {
        Session manager = login("joao", null);
        Session employee = login("maria", manager.companyId());

        mockMvc.perform(auth(get(BUDGETS + "?year=2094"), employee)).andExpect(status().isForbidden());
        send(put(BUDGETS), employee, "{\"year\":2094,\"lines\":[]}").andExpect(status().isForbidden());
        send(post(BUDGETS + "/copy-previous-year"), employee, "{\"year\":2094}").andExpect(status().isForbidden());
        mockMvc.perform(auth(get(BUDGETS + "/variance?year=2094&month=1"), employee))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(get(BUDGETS + "/alerts?year=2094"), employee)).andExpect(status().isForbidden());
    }

    // --- cenário ---

    private long admCenterId(Session s) throws Exception {
        for (JsonNode center : getJson("/api/accounting/cost-centers", s)) {
            if ("ADM".equals(center.get("code").asText())) return center.get("id").asLong();
        }
        throw new AssertionError("Centro ADM não encontrado");
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

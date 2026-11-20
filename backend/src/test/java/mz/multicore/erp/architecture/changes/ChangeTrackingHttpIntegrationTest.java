package mz.multicore.erp.architecture.changes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AA-02, AA-03 — see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §3. */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:change-tracking;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangeTrackingHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void aa02_escritaComSucessoSobeOContadorELeituraEErroNao() throws Exception {
        Session admin = login();
        String company = admin.companies().get(0);

        long start = version(admin, company);

        mockMvc.perform(get("/api/product-categories")
                        .header("Authorization", "Bearer " + admin.token()).header("X-Company-Id", company))
                .andExpect(status().isOk());
        assertThat(version(admin, company)).isEqualTo(start);

        createCategory(admin, company, "{\"code\":\"\",\"name\":\"\"}").andExpect(status().is4xxClientError());
        assertThat(version(admin, company)).isEqualTo(start);

        String code = "AA" + System.nanoTime() % 1_000_000;
        createCategory(admin, company, "{\"code\":\"" + code + "\",\"name\":\"Auto refresh " + code + "\"}")
                .andExpect(status().isOk());
        assertThat(version(admin, company)).isGreaterThan(start);
    }

    @Test
    void aa03_contadorEDaEmpresaActivaEExigeToken() throws Exception {
        Session admin = login();
        assertThat(admin.companies()).hasSizeGreaterThanOrEqualTo(2);
        String first = admin.companies().get(0);
        String second = admin.companies().get(1);

        long otherBefore = version(admin, second);
        String code = "AB" + System.nanoTime() % 1_000_000;
        createCategory(admin, first, "{\"code\":\"" + code + "\",\"name\":\"Isolamento " + code + "\"}")
                .andExpect(status().isOk());

        assertThat(version(admin, second)).isEqualTo(otherBefore);

        mockMvc.perform(get("/api/changes").header("X-Company-Id", first))
                .andExpect(status().isUnauthorized());
    }

    private record Session(String token, List<String> companies) {}

    private Session login() throws Exception {
        JsonNode login = json(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<String> companies = new ArrayList<>();
        login.get("companies").forEach(c -> companies.add(c.get("id").asText()));
        return new Session(login.get("token").asText(), companies);
    }

    private long version(Session session, String company) throws Exception {
        JsonNode body = json(mockMvc.perform(get("/api/changes")
                        .header("Authorization", "Bearer " + session.token()).header("X-Company-Id", company))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(body.get("epoch").asText()).isNotBlank();
        return body.get("version").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions createCategory(Session session, String company, String body)
            throws Exception {
        return mockMvc.perform(post("/api/product-categories")
                .header("Authorization", "Bearer " + session.token()).header("X-Company-Id", company)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}

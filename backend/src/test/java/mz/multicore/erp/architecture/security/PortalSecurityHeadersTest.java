package mz.multicore.erp.architecture.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * PG-09 — as respostas de {@code /portal/**} levam {@code Content-Security-Policy} sem {@code unsafe-inline}
 * nem origens externas, {@code X-Frame-Options: DENY} e {@code Referrer-Policy: no-referrer}
 * (PORTAL_GESTOR_SPEC §3.5). O resto da aplicação mantém o {@code SAMEORIGIN} da consola H2.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-headers;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalSecurityHeadersTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void pg09PortalResponsesCarrySecurityHeaders() throws Exception {
        for (String path : new String[]{"/portal", "/portal/", "/portal/index.html", "/portal/app.js"}) {
            MockHttpServletResponse response = mockMvc.perform(get(path)).andReturn().getResponse();

            assertThat(response.getHeader("X-Frame-Options")).as(path).isEqualTo("DENY");
            assertThat(response.getHeader("Referrer-Policy")).as(path).isEqualTo("no-referrer");
            assertThat(response.getHeader("Content-Security-Policy")).as(path).isNotBlank();
        }
    }

    @Test
    void pg09ContentSecurityPolicyHasNoInlineNorExternalOrigins() throws Exception {
        String csp = mockMvc.perform(get("/portal/index.html")).andReturn().getResponse()
                .getHeader("Content-Security-Policy");

        assertThat(csp).doesNotContain("unsafe-inline", "unsafe-eval", "*", "http:", "https:", "data:");
        assertThat(csp).contains("default-src 'self'", "script-src 'self'", "style-src 'self'",
                "object-src 'none'", "frame-ancestors 'none'", "base-uri 'none'");
        for (String directive : csp.split(";")) {
            String[] tokens = directive.trim().split("\\s+");
            for (int i = 1; i < tokens.length; i++) {
                assertThat(tokens[i]).as(directive).isIn("'self'", "'none'");
            }
        }
    }

    @Test
    void pg09OnlyOnePolicyPerHeader() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/portal/index.html")).andReturn().getResponse();

        assertThat(response.getHeaders("X-Frame-Options")).containsExactly("DENY");
        assertThat(response.getHeaders("Content-Security-Policy")).hasSize(1);
    }

    @Test
    void pg09RestOfApplicationKeepsCurrentFrameOptions() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/api/version")).andReturn().getResponse();

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(response.getHeader("Content-Security-Policy")).isNull();

        MockHttpServletResponse lookalike = mockMvc.perform(get("/portalx/index.html")).andReturn().getResponse();
        assertThat(lookalike.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
        assertThat(lookalike.getHeader("Content-Security-Policy")).isNull();
    }
}

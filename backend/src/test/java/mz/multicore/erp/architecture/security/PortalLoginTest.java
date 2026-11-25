package mz.multicore.erp.architecture.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PG-01 — login com canal PORTAL só para ADMIN/MANAGER (PORTAL_GESTOR_SPEC §3.1). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-login;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pg01PortalLoginAcceptsAdmin() throws Exception {
        portalLogin("ana", "PORTAL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.companies.length()").value(2));
    }

    @Test
    void pg01PortalLoginAcceptsManager() throws Exception {
        portalLogin("joao", "PORTAL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.companies[0].role").value("MANAGER"));
    }

    @Test
    void pg01PortalLoginRejectsEmployeeWithoutToken() throws Exception {
        portalLogin("maria", "PORTAL")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message").value(PortalLoginPolicy.ROLE_REFUSED_MESSAGE));
    }

    @Test
    void pg01ChannelIsCaseInsensitive() throws Exception {
        portalLogin("maria", "portal").andExpect(status().isBadRequest());
    }

    @Test
    void pg01DesktopLoginUnchangedForEmployee() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"maria","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private ResultActions portalLogin(String username, String channel) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"password","channel":"%s"}
                        """.formatted(username, channel)));
    }
}

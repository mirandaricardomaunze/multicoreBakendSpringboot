package com.phcpro.modules.hr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida, pela API HTTP, a exposição REST do mapa fiscal salarial e da configuração de
 * impostos (item 4 da punch list de RH / cenários RH-23, RH-24, RH-25 do harness):
 * token obrigatório (401), empresa validada por acesso (403) e leitura autenticada (200).
 *
 * Utilizadores semeados (DataLoader): maria=EMPLOYEE (só PT), ana=ADMIN (PT+MZ). Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hr-api;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HRApiIntegrationTest {

    private static final String PT_TAX_ID = "501982736";
    private static final String MZ_TAX_ID = "400123456";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;

    @Test
    void mapaFiscal_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/hr/payroll/fiscal-summary/2026/6"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void taxConfig_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/hr/payroll/tax-config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void taxConfig_empresaSemAcesso_retorna403() throws Exception {
        String token = login("maria"); // só tem acesso à empresa PT
        mockMvc.perform(get("/api/hr/payroll/tax-config")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId(MZ_TAX_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void taxConfig_autenticado_retorna200() throws Exception {
        String token = login("ana"); // ADMIN com acesso à empresa PT
        mockMvc.perform(get("/api/hr/payroll/tax-config")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId(PT_TAX_ID)))
                .andExpect(status().isOk());
    }

    // ────────────────────────── helpers ──────────────────────────

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"password\":\"password\"}", username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }

    private String companyId(String taxId) {
        return companyRepository.findAll().stream()
                .filter(c -> taxId.equals(c.getTaxId()))
                .findFirst()
                .orElseThrow()
                .getId()
                .toString();
    }
}

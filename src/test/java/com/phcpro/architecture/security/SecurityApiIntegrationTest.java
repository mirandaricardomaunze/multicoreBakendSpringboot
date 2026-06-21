package com.phcpro.architecture.security;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida ponta-a-ponta, pela API HTTP, as três camadas de segurança da spec §9:
 * token obrigatório (401), empresa validada por acesso (403) e role no Service para
 * operações sensíveis (faturar exige MANAGER/ADMIN). Fecha o item "login, tenant e
 * roles testados por API" do harness.
 *
 * Utilizadores semeados (DataLoader): maria=EMPLOYEE (só PT), joao=MANAGER (PT),
 * ana=ADMIN (PT+MZ). Password "password".
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security-api;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityApiIntegrationTest {

    private static final String PT_TAX_ID = "501982736";
    private static final String MZ_TAX_ID = "400123456";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompanyRepository companyRepository;

    @Test
    void endpointProtegido_semToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/comercial/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void empresaSemAcesso_retorna403() throws Exception {
        String token = login("maria"); // maria só tem acesso à empresa PT
        mockMvc.perform(get("/api/comercial/clients")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId(MZ_TAX_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void employee_aoFaturar_eBloqueadoPelaPermissao() throws Exception {
        String token = login("maria"); // EMPLOYEE
        mockMvc.perform(post("/api/comercial/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId(PT_TAX_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody(companyId(PT_TAX_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("perfil")));
    }

    @Test
    void admin_aoFaturar_passaAPermissao() throws Exception {
        String token = login("ana"); // ADMIN — passa a guarda de role
        // Dados inexistentes: falha mais à frente ("Cliente não encontrado"), NÃO por permissão.
        mockMvc.perform(post("/api/comercial/invoices")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId(PT_TAX_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody(companyId(PT_TAX_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("perfil"))))
                .andExpect(jsonPath("$.message", containsString("Cliente")));
    }

    // ────────────────────────── helpers ──────────────────────────

    private String invoiceBody(String companyId) {
        return String.format("""
                {
                  "clientId": 999999,
                  "companyId": %s,
                  "warehouseId": 999999,
                  "lines": [ { "productId": 999999, "quantity": 1, "taxRate": 0.16 } ]
                }
                """, companyId);
    }

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

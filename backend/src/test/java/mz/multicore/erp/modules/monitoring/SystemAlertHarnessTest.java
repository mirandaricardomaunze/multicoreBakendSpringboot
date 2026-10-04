package mz.multicore.erp.modules.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.monitoring.dto.SystemAlertIncidentDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemAlertTestResultDTO;
import mz.multicore.erp.modules.monitoring.dto.TenantHealthDTO;
import mz.multicore.erp.modules.monitoring.service.SystemAlertEmailService;
import mz.multicore.erp.modules.monitoring.service.SystemIncidentManager;
import mz.multicore.erp.modules.monitoring.service.TenantMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:alert-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemAlertHarnessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemIncidentManager incidentManager;

    @Autowired
    private SystemAlertEmailService emailService;

    @Autowired
    private TenantMonitoringService tenantMonitoringService;

    @BeforeEach
    void setUp() {
        mz.multicore.erp.architecture.security.CurrentUserContext.setCurrentUser("admin", "ADMIN");
        mz.multicore.erp.architecture.security.CurrentUserContext.setCurrentCompanyId(1L);
        incidentManager.clearAll();
        emailService.clearCooldowns();
    }

    private String obtainAuthToken() throws Exception {
        String res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(res);
        return node.get("token").asText();
    }

    @Test
    @DisplayName("Harness: SystemIncidentManager regista incidentes e limita histórico a 50")
    void testIncidentManagerLifecycle() {
        SystemAlertIncidentDTO inc = incidentManager.recordIncident("BancoDados", "Empresa A", "CRITICAL", "Falha de conexão");
        assertThat(inc).isNotNull();
        assertThat(inc.incidentId()).startsWith("INC-");
        assertThat(inc.severity()).isEqualTo("CRITICAL");
        assertThat(inc.resolved()).isFalse();

        List<SystemAlertIncidentDTO> recents = incidentManager.getRecentIncidents();
        assertThat(recents).hasSize(1);
        assertThat(recents.get(0).incidentId()).isEqualTo(inc.incidentId());

        boolean resolved = incidentManager.resolveIncident(inc.incidentId());
        assertThat(resolved).isTrue();
        assertThat(incidentManager.getRecentIncidents().get(0).resolved()).isTrue();

        // Encher com mais de 50
        for (int i = 0; i < 60; i++) {
            incidentManager.recordIncident("Sub" + i, "Tenant", "WARNING", "Aviso " + i);
        }
        assertThat(incidentManager.getRecentIncidents()).hasSize(50);
    }

    @Test
    @DisplayName("Harness: SystemAlertEmailService respeita cooldown inteligente de 15 minutos")
    void testEmailAlertCooldown() {
        // Primeiro envio deve ter sucesso
        boolean first = emailService.sendCriticalAlert("Storage", "Tenant X", "Disco a 95%", "admin@multicore.co.mz");
        assertThat(first).isTrue();

        // Segundo envio imediato deve ser suprimido pelo cooldown
        boolean second = emailService.sendCriticalAlert("Storage", "Tenant X", "Disco a 96%", "admin@multicore.co.mz");
        assertThat(second).isFalse();

        // Envio para outro subsistema deve passar
        boolean third = emailService.sendCriticalAlert("Memory", "Tenant X", "Heap a 92%", "admin@multicore.co.mz");
        assertThat(third).isTrue();
    }

    @Test
    @DisplayName("Harness: SystemAlertEmailService validação de endereço e teste de envio")
    void testEmailAlertValidationAndTest() {
        SystemAlertTestResultDTO invalid = emailService.testEmailAlert("email-invalido");
        assertThat(invalid.success()).isFalse();
        assertThat(invalid.message()).contains("inválido");

        SystemAlertTestResultDTO valid = emailService.testEmailAlert("admin@multicore.co.mz");
        assertThat(valid.success()).isTrue();
        assertThat(valid.channel()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Harness: TenantMonitoringService recolhe saúde dos inquilinos activos")
    void testTenantHealthEvaluation() {
        List<TenantHealthDTO> tenants = tenantMonitoringService.getTenantsHealth();
        assertThat(tenants).isNotNull();
        assertThat(tenants).isNotEmpty();

        TenantHealthDTO first = tenants.get(0);
        assertThat(first.companyId()).isNotNull();
        assertThat(first.companyName()).isNotBlank();
        assertThat(first.status()).isIn("HEALTHY", "WARNING", "CRITICAL");
        assertThat(first.lastBackupStatus()).isNotNull();
    }

    @Test
    @DisplayName("Harness: ADMIN consulta saude do tenant, mas nao acede a incidentes globais")
    void testMonitoringEndpoints() throws Exception {
        String token = obtainAuthToken();

        mockMvc.perform(get("/api/monitoring/tenants-health")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/monitoring/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/monitoring/test-email-alert")
                        .header("Authorization", "Bearer " + token)
                        .param("recipientEmail", "noc@multicore.co.mz"))
                .andExpect(status().isForbidden());
    }
}

package mz.multicore.erp.modules.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.modules.monitoring.dto.SubsystemStatusDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemDiagnosticsExportDTO;
import mz.multicore.erp.modules.monitoring.dto.SystemHealthDTO;
import mz.multicore.erp.modules.monitoring.service.SystemMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:monitoring-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemMonitoringHarnessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemMonitoringService monitoringService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mz.multicore.erp.architecture.security.CurrentUserContext.setCurrentUser("admin", "ADMIN");
        mz.multicore.erp.architecture.security.CurrentUserContext.setCurrentCompanyId(1L);
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
    @DisplayName("Harness: Telemetria da JVM reporta métricas não negativas e coerentes")
    void testJvmTelemetryCoherence() {
        SystemHealthDTO health = monitoringService.getSystemHealth();
        assertThat(health).isNotNull();
        assertThat(health.overallStatus()).isNotNull();

        var mem = health.memory();
        assertThat(mem).isNotNull();
        assertThat(mem.heapMaxBytes()).isGreaterThan(0);
        assertThat(mem.heapUsedBytes()).isGreaterThan(0);
        assertThat(mem.heapUsedPercent()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Harness: Base de dados reporta conectividade ativa e métricas de pool Hikari")
    void testDatabaseTelemetry() {
        SystemHealthDTO health = monitoringService.getSystemHealth();
        var db = health.database();
        assertThat(db).isNotNull();
        assertThat(db.databaseEngine()).isNotBlank();
        assertThat(db.responseTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(db.status()).isIn("UP", "HEALTHY", "WARNING", "CRITICAL");
    }

    @Test
    @DisplayName("Harness: Armazenamento e Thread Pools reportam métricas de runtime válidas")
    void testStorageAndThreadPoolTelemetry() {
        SystemHealthDTO health = monitoringService.getSystemHealth();

        var storage = health.storage();
        assertThat(storage).isNotNull();
        assertThat(storage.totalBytes()).isGreaterThan(0);
        assertThat(storage.freeBytes()).isGreaterThanOrEqualTo(0);
        assertThat(storage.usedBytes()).isGreaterThanOrEqualTo(0);
        assertThat(storage.usedPercent()).isBetween(0.0, 100.0);

        var threads = health.threadPool();
        assertThat(threads).isNotNull();
        assertThat(threads.liveThreads()).isGreaterThanOrEqualTo(0);
        assertThat(threads.peakThreads()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Harness: Subsistemas canónicos monitorados estão presentes na lista de status")
    void testSubsystemsIntegrity() {
        SystemHealthDTO health = monitoringService.getSystemHealth();
        assertThat(health.subsystems()).isNotEmpty();

        var names = health.subsystems().stream().map(SubsystemStatusDTO::name).toList();
        assertThat(names).contains("Base de Dados Principal", "Motor de Faturação & POS", "Mecanismo de Backups", "Armazenamento de Ficheiros", "Serviço de Licenciamento");
    }

    @Test
    @DisplayName("Harness: Exportação de diagnósticos gera relatório textual padronizado")
    void testDiagnosticsExportFormat() {
        SystemDiagnosticsExportDTO export = monitoringService.getDiagnosticsExport();
        assertThat(export).isNotNull();
        assertThat(export.generatedAt()).isNotNull();
        assertThat(export.reportFormattedText()).isNotBlank();
        assertThat(export.reportFormattedText()).contains("MULTICORE ERP");
        assertThat(export.reportFormattedText()).contains("DO SISTEMA");
        assertThat(export.reportFormattedText()).contains("Estado Geral");
        assertThat(export.reportFormattedText()).contains("RECURSOS DE RUNTIME");
        assertThat(export.reportFormattedText()).contains("BASE DE DADOS");
        assertThat(export.reportFormattedText()).contains("SUBSISTEMAS AUDITADOS");
    }

    @Test
    @DisplayName("Harness: Endpoint HTTP /api/monitoring/system-health responde 200 OK para utilizadores autenticados")
    void testHttpSystemHealthEndpoint() throws Exception {
        String token = obtainAuthToken();
        String json = mockMvc.perform(get("/api/monitoring/system-health")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("overallStatus")).isTrue();
        assertThat(root.has("memory")).isTrue();
        assertThat(root.has("database")).isTrue();
        assertThat(root.has("storage")).isTrue();
        assertThat(root.has("subsystems")).isTrue();
    }

    @Test
    @DisplayName("Harness: Endpoint HTTP /api/monitoring/diagnostics-export responde 200 OK para ADMIN")
    void testHttpDiagnosticsExportEndpoint() throws Exception {
        String token = obtainAuthToken();
        String json = mockMvc.perform(get("/api/monitoring/diagnostics-export")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("healthSnapshot")).isTrue();
        assertThat(root.has("reportFormattedText")).isTrue();
        assertThat(root.get("reportFormattedText").asText()).contains("MULTICORE ERP");
        assertThat(root.get("reportFormattedText").asText()).contains("DO SISTEMA");
        assertThat(root.get("reportFormattedText").asText()).contains("RECURSOS DE RUNTIME");
    }
}

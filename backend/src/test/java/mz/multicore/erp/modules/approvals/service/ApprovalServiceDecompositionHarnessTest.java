package mz.multicore.erp.modules.approvals.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de ApprovalService (Fase 122).
 */
class ApprovalServiceDecompositionHarnessTest {

    private static final String BASE = "src/main/java/mz/multicore/erp/modules/approvals/service/";
    private static final List<String> FILES = List.of(
            "ApprovalService.java",
            "ApprovalLifecycleService.java",
            "ApprovalQueryService.java",
            "ApprovalEscalationPolicy.java"
    );

    private static String readSource(String file) {
        try {
            return Files.readString(Path.of(BASE + file));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + file, e);
        }
    }

    private static long countLines(String file) {
        try (var lines = Files.lines(Path.of(BASE + file))) {
            return lines.count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + file, e);
        }
    }

    @Test
    @DisplayName("APSD-01: Limites físicos estritos de linhas de código")
    void apsd01_limitesFisicosLinhasCodigo() {
        assertThat(countLines("ApprovalService.java")).as("Fachada <= 115").isLessThanOrEqualTo(115);
        assertThat(countLines("ApprovalLifecycleService.java")).as("Ciclo de vida <= 215").isLessThanOrEqualTo(215);
        assertThat(countLines("ApprovalQueryService.java")).as("Consultas <= 100").isLessThanOrEqualTo(100);
        assertThat(countLines("ApprovalEscalationPolicy.java")).as("Política <= 50").isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("APSD-02: Backend headless sem Swing nem JOptionPane")
    void apsd02_backendHeadless() {
        for (String file : FILES) {
            assertThat(readSource(file)).as(file).doesNotContain("JOptionPane", "javax.swing");
        }
    }

    @Test
    @DisplayName("APSD-03: Delegação canónica e alçadas nos colaboradores")
    void apsd03_delegacaoCanonica() {
        assertThat(readSource("ApprovalService.java"))
                .contains("lifecycleService.submitRequest(")
                .contains("lifecycleService.approveRequest(")
                .contains("lifecycleService.rejectRequest(")
                .contains("lifecycleService.cancelPendingForDocument(")
                .contains("queryService.getPendingRequests(")
                .contains("queryService.getAllRequests(");
        assertThat(readSource("ApprovalLifecycleService.java"))
                .contains("triggerCallback(")
                .contains("logHistory(")
                .contains("requestRepository.save(");
        assertThat(readSource("ApprovalQueryService.java"))
                .contains("syncUnindexedPendingDocuments(");
        assertThat(readSource("ApprovalEscalationPolicy.java"))
                .contains("BASE_AUTO_APPROVE_LIMIT")
                .contains("BASE_MANAGER_LIMIT");
    }

    @Test
    @DisplayName("APSD-04: Contratos públicos e construtores retrocompatíveis preservados")
    void apsd04_contratosPreservados() {
        assertThat(readSource("ApprovalService.java"))
                .contains("@Autowired")
                .contains("public static final BigDecimal BASE_AUTO_APPROVE_LIMIT")
                .contains("public static final BigDecimal BASE_MANAGER_LIMIT")
                .contains("public ApprovalRequestDTO submitRequest(String documentType, Long documentId, BigDecimal amount, String description)")
                .contains("public ApprovalRequestDTO approveRequest(Long requestId, String comments)")
                .contains("public ApprovalRequestDTO rejectRequest(Long requestId, String reason)")
                .contains("public void cancelPendingForDocument(String documentType, Long documentId, String reason)")
                .contains("CompanyLookup companyLookup)")
                .contains("ObjectProvider<StockWasteRepository> stockWasteRepository)");
    }
}

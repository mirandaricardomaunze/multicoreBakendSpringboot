package mz.multicore.erp.modules.intelligence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural de docs/AI_MANAGEMENT_ASSISTANT_SPEC.md (AI-01..AI-07). */
class AiManagementAssistantHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend")) ? Path.of("") : Path.of("..");

    @Test
    void contractsAreRecordsAndStayFrameworkNeutral() throws Exception {
        String request = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiAssistantRequest.java");
        String response = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiAssistantResponse.java");
        assertThat(request).contains("record AiAssistantRequest", "@Size(min = 3, max = 600,")
                .doesNotContain("springframework", "jakarta.persistence", "javax.swing");
        assertThat(response).contains("record AiAssistantResponse", "externalModelUsed", "AiEvidenceDTO")
                .doesNotContain("apiKey", "token", "Entity");
    }

    @Test
    void backendEnforcesLayerTenantPermissionAuditAndSafeFallback() throws Exception {
        String controller = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/controller/AiManagementAssistantController.java");
        String service = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/AiManagementAssistantService.java");
        String gateway = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/OptionalExternalManagementAiGateway.java");
        assertThat(controller).contains("service.ask(request)")
                .doesNotContain("Repository", "jakarta.persistence", "modules.company.model");
        assertThat(service).contains("PermissionGuard.requireManagerOrAdmin", "CurrentUserContext.getCurrentCompanyId()")
                .contains("AI_MANAGEMENT_QUERY", "aiGateway.answer", "localAnswer")
                .contains("findByWarehouseCompanyId(companyId)", "findByCompanyIdOrderByOrderDateDesc(companyId)")
                .doesNotContain("getClient()", "getSupplier()", "getUsername()");
        assertThat(gateway).contains("if (!configured()) return Optional.empty()", "https")
                .contains("ManagementAiContext").doesNotContain("Company", "Invoice", "Stock", "PurchaseOrder");
    }

    @Test
    void desktopUsesOnlyHttpsContractAndCanonicalAsyncUi() throws Exception {
        String client = source("desktop/src/main/java/mz/multicore/erp/desktop/client/AiAssistantApiClient.java");
        String panel = source("desktop/src/main/java/mz/multicore/erp/gui/AiManagementAssistantPanel.java");
        String frame = source("desktop/src/main/java/mz/multicore/erp/gui/MainFrame.java");
        assertThat(client).contains("DesktopClientFactory", "/api/intelligence/management-assistant/ask")
                .doesNotContain("API_KEY", "MULTICORE_AI", "HttpClient");
        assertThat(panel).contains("ModernPanel(16)", "UIHelper.submitAsync", "InlineFeedbackPanel")
                .contains("A IA apenas recomenda", "AiEvidenceDTO")
                .doesNotContain("JOptionPane", "Repository");
        assertThat(frame).contains("new AiManagementAssistantPanel(aiAssistantApiClient, this::navigate)")
                .contains("Assistente de Gestão", "assistente_ia");
    }

    @Test
    void professionalReportIsTenantProtectedAuditedAndPreviewed() throws Exception {
        String controller = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/controller/AiManagementAssistantController.java");
        String report = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/AiManagementReportService.java");
        String client = source("desktop/src/main/java/mz/multicore/erp/desktop/client/AiAssistantApiClient.java");
        String panel = source("desktop/src/main/java/mz/multicore/erp/gui/AiManagementAssistantPanel.java");
        assertThat(controller).contains("/report.pdf", "reportService.render(response)");
        assertThat(report).contains("PermissionGuard.requireManagerOrAdmin", "CurrentUserContext.getCurrentCompanyId()")
                .contains("CompanyHeaderRenderer", "AI_MANAGEMENT_REPORT_GENERATED")
                .contains("Não substitui documentos fiscais");
        assertThat(client).contains("postForBytes", "/report.pdf");
        assertThat(panel).contains("Gerar Relatório PDF", "PrintPreviewDialog.show");
    }

    @Test
    void flexibleDocumentsUseAllowListAndOperationalBoundary() throws Exception {
        String contract = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiProfessionalDocumentRequest.java");
        String report = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/AiManagementReportService.java");
        String panel = source("desktop/src/main/java/mz/multicore/erp/gui/AiManagementAssistantPanel.java");
        assertThat(contract).contains("record AiProfessionalDocumentRequest").doesNotContain("springframework", "persistence");
        assertThat(report).contains("ALLOWED_TYPES", "renderProfessionalDocument", "AI_PROFESSIONAL_DOCUMENT_GENERATED")
                .contains("módulo operacional correspondente");
        assertThat(panel).contains("Criar Documento Profissional", "Gerar Documento", "AiProfessionalDocumentRequest");
    }

    @Test
    void specCoversAllHarnessCriteria() throws Exception {
        String spec = source("docs/AI_MANAGEMENT_ASSISTANT_SPEC.md");
        assertThat(spec).contains("SPEC-AI-MANAGEMENT-ASSISTANT-001", "AI-01", "AI-02", "AI-03",
                "AI-04", "AI-05", "AI-06", "AI-07", "AI-08", "AI-09", "AI-10", "BigDecimal", "fallback local");
    }

    private static String source(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}

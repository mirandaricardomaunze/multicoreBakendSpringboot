package mz.multicore.erp.modules.intelligence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AiDocumentLifecycleHarnessTest {
    private static final Path ROOT = Files.isDirectory(Path.of("backend")) ? Path.of("") : Path.of("..");

    @Test
    void contractsRemainNeutralAndVersioned() throws Exception {
        String document = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiManagedDocumentDTO.java");
        String version = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiDocumentVersionDTO.java");
        assertThat(document).contains("record AiManagedDocumentDTO", "currentVersion", "reviewedBy")
                .doesNotContain("springframework", "persistence", "swing");
        assertThat(version).contains("record AiDocumentVersionDTO", "versionNumber", "generatedContent")
                .doesNotContain("springframework", "persistence", "swing");
    }

    @Test
    void migrationIsAdditiveTenantScopedAndKeepsImmutableVersions() throws Exception {
        assertThat(source("backend/src/main/resources/db/migration/V86__ai_document_history_and_approval.sql"))
                .contains("CREATE TABLE IF NOT EXISTS ai_documents", "company_id BIGINT NOT NULL",
                        "CREATE TABLE IF NOT EXISTS ai_document_versions", "uk_ai_document_version",
                        "pdf_data BYTEA NOT NULL")
                .doesNotContain("DROP TABLE", "TRUNCATE", "DELETE FROM");
    }

    @Test
    void serviceEnforcesTenantRolesSegregationTransitionsAndSafeAudit() throws Exception {
        String service = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/AiDocumentLifecycleService.java");
        assertThat(service).contains("PermissionGuard.requireManagerOrAdmin", "PermissionGuard.requireAdmin",
                        "findByIdAndCompanyId", "EM_REVISAO", "APROVADO", "REJEITADO", "ARQUIVADO",
                        "O autor nao pode aprovar", "AI_DOCUMENT_VERSION_CREATED", "AI_DOCUMENT_EXPORTED")
                .doesNotContain("pdfData=", "generatedContent=", "instructions=");
    }

    @Test
    void controllerDelegatesAndNeverTouchesPersistence() throws Exception {
        assertThat(source("backend/src/main/java/mz/multicore/erp/modules/intelligence/controller/AiDocumentLifecycleController.java"))
                .contains("/api/intelligence/documents", "service.create(request)", "service.approve(id)",
                        "service.reject(id, request)", "service.pdf(id, version)")
                .doesNotContain("Repository", "EntityManager", "jakarta.persistence");
    }

    @Test
    void desktopUsesHttpAndStandardTableCard() throws Exception {
        String client = source("desktop/src/main/java/mz/multicore/erp/desktop/client/AiAssistantApiClient.java");
        String panel = source("desktop/src/main/java/mz/multicore/erp/gui/AiManagementAssistantPanel.java");
        assertThat(client).contains("/api/intelligence/documents", "getDocumentPdf", "DesktopClientFactory")
                .doesNotContain("Repository", "JpaRepository");
        assertThat(panel).contains("Histórico e Aprovação", "ModernPanel(16)", "Fluxo do Documento",
                        "Guardar no Histórico", "Comparar versões", "Criar nova versão",
                        "UIHelper.styleTable(documentTable)")
                .doesNotContain("JOptionPane");
    }

    @Test
    void specDefinesEveryAcceptanceCriterion() throws Exception {
        assertThat(source("docs/AI_DOCUMENT_LIFECYCLE_SPEC.md"))
                .contains("SPEC-AI-DOCUMENT-LIFECYCLE-001", "DL-01", "DL-02", "DL-03", "DL-04",
                        "DL-05", "DL-06", "DL-07", "DL-08", "DL-09", "DL-10", "DL-11", "DL-12");
    }

    private static String source(String relative) throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}

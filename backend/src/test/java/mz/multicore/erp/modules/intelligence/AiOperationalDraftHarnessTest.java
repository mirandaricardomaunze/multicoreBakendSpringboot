package mz.multicore.erp.modules.intelligence;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class AiOperationalDraftHarnessTest {
    private static final Path ROOT = Files.isDirectory(Path.of("backend")) ? Path.of("") : Path.of("..");

    @Test void contractsAreNeutralRecords() throws Exception {
        String request = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiOperationalDraftRequest.java");
        String response = source("contracts/src/main/java/mz/multicore/erp/modules/intelligence/dto/AiOperationalDraftResponse.java");
        assertThat(request).contains("record AiOperationalDraftRequest").doesNotContain("springframework", "persistence", "swing");
        assertThat(response).contains("explicitFields", "missingFields", "targetModule").doesNotContain("springframework", "persistence");
    }

    @Test void serviceIsNonPersistentTenantProtectedAndAudited() throws Exception {
        String service = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/service/AiOperationalDraftService.java");
        assertThat(service).contains("PermissionGuard.requireManagerOrAdmin", "CurrentUserContext.getCurrentCompanyId()")
                .contains("AI_OPERATIONAL_DRAFT_PREPARED", "QUOTATION", "PURCHASE", "COLLECTION", "REPLENISHMENT", "PROPOSAL")
                .doesNotContain("Repository", ".save(", "InvoiceService", "StockService");
    }

    @Test void controllerAndDesktopKeepReviewBoundary() throws Exception {
        String controller = source("backend/src/main/java/mz/multicore/erp/modules/intelligence/controller/AiManagementAssistantController.java");
        String panel = source("desktop/src/main/java/mz/multicore/erp/gui/AiManagementAssistantPanel.java");
        assertThat(controller).contains("/operational-draft", "draftService.prepare(request)");
        assertThat(panel).contains("Rascunho Operacional Controlado", "Preparar Rascunho", "Abrir no Módulo")
                .contains("UIHelper.submitAsync", "navigationHandler.accept");
    }

    @Test void specDefinesAllCriteria() throws Exception {
        assertThat(source("docs/AI_OPERATIONAL_DRAFT_SPEC.md"))
                .contains("OD-01", "OD-02", "OD-03", "OD-04", "OD-05", "OD-06", "OD-07", "OD-08");
    }

    private static String source(String relative) throws Exception { return Files.readString(ROOT.resolve(relative)); }
}

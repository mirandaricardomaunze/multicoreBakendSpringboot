package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.intelligence.dto.AiAssistantRequest;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiProfessionalDocumentRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftRequest;
import mz.multicore.erp.modules.intelligence.dto.AiOperationalDraftResponse;
import mz.multicore.erp.modules.intelligence.dto.AiDocumentReviewRequest;
import mz.multicore.erp.modules.intelligence.dto.AiDocumentVersionDTO;
import mz.multicore.erp.modules.intelligence.dto.AiManagedDocumentDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Profile("desktop")
public class AiAssistantApiClient {

    private final DesktopClientFactory clientFactory;

    public AiAssistantApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public AiAssistantResponse ask(String question) {
        return clientFactory.authenticatedClient().post(
                "/api/intelligence/management-assistant/ask",
                new AiAssistantRequest(question), AiAssistantResponse.class);
    }

    public byte[] renderReport(AiAssistantResponse response) {
        return clientFactory.authenticatedClient().postForBytes(
                "/api/intelligence/management-assistant/report.pdf", response);
    }

    public byte[] renderDocument(AiProfessionalDocumentRequest request) {
        return clientFactory.authenticatedClient().postForBytes(
                "/api/intelligence/management-assistant/document.pdf", request);
    }

    public AiOperationalDraftResponse prepareOperationalDraft(String request) {
        return clientFactory.authenticatedClient().post(
                "/api/intelligence/management-assistant/operational-draft",
                new AiOperationalDraftRequest(request), AiOperationalDraftResponse.class);
    }

    public AiManagedDocumentDTO saveDocument(AiProfessionalDocumentRequest request) {
        return clientFactory.authenticatedClient().post("/api/intelligence/documents", request, AiManagedDocumentDTO.class);
    }

    public List<AiManagedDocumentDTO> searchDocuments(String query) {
        String encoded = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient().getList("/api/intelligence/documents?query=" + encoded,
                AiManagedDocumentDTO.class);
    }

    public List<AiDocumentVersionDTO> documentVersions(Long id) {
        return clientFactory.authenticatedClient().getList("/api/intelligence/documents/" + id + "/versions",
                AiDocumentVersionDTO.class);
    }

    public AiManagedDocumentDTO createDocumentVersion(Long id, AiProfessionalDocumentRequest request) {
        return clientFactory.authenticatedClient().post(
                "/api/intelligence/documents/" + id + "/versions", request, AiManagedDocumentDTO.class);
    }

    public AiManagedDocumentDTO submitDocument(Long id) {
        return action(id, "submit", null);
    }

    public AiManagedDocumentDTO approveDocument(Long id) {
        return action(id, "approve", null);
    }

    public AiManagedDocumentDTO rejectDocument(Long id, String reason) {
        return action(id, "reject", new AiDocumentReviewRequest(reason));
    }

    public AiManagedDocumentDTO archiveDocument(Long id) {
        return action(id, "archive", null);
    }

    public byte[] getDocumentPdf(Long id, int version) {
        return clientFactory.authenticatedClient().getBytes(
                "/api/intelligence/documents/" + id + "/versions/" + version + "/document.pdf");
    }

    private AiManagedDocumentDTO action(Long id, String action, Object body) {
        return clientFactory.authenticatedClient().post(
                "/api/intelligence/documents/" + id + "/" + action, body, AiManagedDocumentDTO.class);
    }
}

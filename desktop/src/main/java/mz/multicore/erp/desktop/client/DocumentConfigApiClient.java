package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Cliente HTTP para a configuração de colunas dos documentos ({@code /api/documents}). */
@Component
@Profile("desktop")
public class DocumentConfigApiClient {

    private final DesktopClientFactory clientFactory;

    public DocumentConfigApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public DocumentColumnsDTO getColumns(Long companyId, DocumentType documentType) {
        return clientFactory.authenticatedClient().get(
                "/api/documents/columns?companyId=" + companyId + "&documentType=" + documentType.name(),
                DocumentColumnsDTO.class);
    }

    public DocumentColumnsDTO save(Long companyId, DocumentType documentType, DocumentColumnsDTO dto) {
        return clientFactory.authenticatedClient().put(
                "/api/documents/columns?companyId=" + companyId + "&documentType=" + documentType.name(),
                dto, DocumentColumnsDTO.class);
    }
}

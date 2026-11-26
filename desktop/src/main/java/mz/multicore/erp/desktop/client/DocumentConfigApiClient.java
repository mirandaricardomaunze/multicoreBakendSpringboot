package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Cliente HTTP para as colunas e o modelo dos documentos ({@code /api/documents}). */
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

    public DocumentTemplateDTO getTemplate(Long companyId, DocumentType documentType) {
        return clientFactory.authenticatedClient().get(templatePath(companyId, documentType), DocumentTemplateDTO.class);
    }

    /** Só ADMIN; o backend valida e audita (MODELOS_DOCUMENTO_SPEC §3.3). */
    public DocumentTemplateDTO saveTemplate(Long companyId, DocumentType documentType, DocumentTemplateDTO dto) {
        return clientFactory.authenticatedClient().put(templatePath(companyId, documentType), dto, DocumentTemplateDTO.class);
    }

    /** Factura de exemplo em PDF com o modelo pedido, sem gravar (MODELOS_DOCUMENTO_SPEC §3.4). */
    public byte[] previewTemplate(Long companyId, DocumentTemplateDTO dto) {
        return clientFactory.authenticatedClient().postForBytes(
                "/api/documents/template/preview?companyId=" + companyId, dto);
    }

    private static String templatePath(Long companyId, DocumentType documentType) {
        return "/api/documents/template?companyId=" + companyId + "&documentType=" + documentType.name();
    }
}

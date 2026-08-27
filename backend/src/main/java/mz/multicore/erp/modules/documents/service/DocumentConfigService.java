package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.model.DocumentColumnConfig;
import mz.multicore.erp.modules.documents.model.DocumentType;
import mz.multicore.erp.modules.documents.repository.DocumentColumnConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuração (por empresa) das colunas visíveis na tabela de linhas dos documentos comerciais.
 * Ausência de configuração = todas as colunas visíveis (default {@link DocumentColumnsDTO#all()}).
 */
@Service
public class DocumentConfigService {

    private final DocumentColumnConfigRepository repository;
    private final AuditLogService auditLogService;

    public DocumentConfigService(DocumentColumnConfigRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public DocumentColumnsDTO getColumns(Long companyId, DocumentType documentType) {
        CurrentUserContext.requireCompany(companyId);
        return repository.findByCompanyIdAndDocumentType(companyId, documentType)
                .map(this::toDTO)
                .orElseGet(DocumentColumnsDTO::all);
    }

    @Transactional
    public DocumentColumnsDTO save(Long companyId, DocumentType documentType, DocumentColumnsDTO dto) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin("configurar colunas de documentos");

        if (!dto.anyVisible()) {
            throw new BusinessRuleException("O documento tem de ter pelo menos uma coluna.");
        }

        DocumentColumnConfig config = repository.findByCompanyIdAndDocumentType(companyId, documentType)
                .orElseGet(() -> {
                    DocumentColumnConfig created = new DocumentColumnConfig();
                    created.setCompanyId(companyId);
                    created.setDocumentType(documentType);
                    created.setCreatedBy("SYSTEM");
                    return created;
                });

        config.setShowBarcode(dto.barcode());
        config.setShowReference(dto.reference());
        config.setShowDescription(dto.description());
        config.setShowExpiry(dto.expiry());
        config.setShowQuantity(dto.quantity());
        config.setShowUnitPrice(dto.unitPrice());
        config.setShowTax(dto.tax());
        config.setShowSubtotal(dto.subtotal());
        config.setFooterComment(dto.footer() == null || dto.footer().isBlank() ? null : dto.footer().trim());

        DocumentColumnConfig saved = repository.save(config);
        auditLogService.logCurrent("DOCUMENT_COLUMNS_UPDATE",
                "Colunas de documentos actualizadas (" + documentType + ").");
        return toDTO(saved);
    }

    private DocumentColumnsDTO toDTO(DocumentColumnConfig config) {
        return new DocumentColumnsDTO(
                config.isShowBarcode(),
                config.isShowReference(),
                config.isShowDescription(),
                config.isShowExpiry(),
                config.isShowQuantity(),
                config.isShowUnitPrice(),
                config.isShowTax(),
                config.isShowSubtotal(),
                config.getFooterComment()
        );
    }
}

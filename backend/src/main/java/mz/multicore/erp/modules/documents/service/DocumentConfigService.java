package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentColumnsDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
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

    /**
     * Modelo visual (cor, logótipo, notas, bancos, QR) que os documentos impressos a partir de agora usam
     * (MODELOS_DOCUMENTO_SPEC §3.3). Sem configuração = {@link DocumentTemplateDTO#defaults()}.
     */
    @Transactional(readOnly = true)
    public DocumentTemplateDTO getTemplate(Long companyId, DocumentType documentType) {
        CurrentUserContext.requireCompany(companyId);
        return repository.findByCompanyIdAndDocumentType(companyId, documentType)
                .map(this::toTemplateDTO)
                .orElseGet(DocumentTemplateDTO::defaults);
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
        config.setShowPackages(dto.packages());
        config.setShowBoxes(dto.boxes());
        config.setShowBoxPercentage(dto.boxPercentage());
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
                config.isShowPackages(),
                config.isShowBoxes(),
                config.isShowBoxPercentage(),
                config.isShowUnitPrice(),
                config.isShowTax(),
                config.isShowSubtotal(),
                config.getFooterComment()
        );
    }

    private DocumentTemplateDTO toTemplateDTO(DocumentColumnConfig config) {
        return new DocumentTemplateDTO(
                config.getAccentColor(),
                config.getLogoPosition(),
                config.getTemplateNotes(),
                config.getBankAccounts().stream()
                        .map(a -> new DocumentBankAccountDTO(a.getBankName(), a.getHolder(), a.getAccountNumber()))
                        .toList(),
                config.isShowQrCode());
    }
}

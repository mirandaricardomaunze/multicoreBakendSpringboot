package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.modules.printing.model.DocumentPrintControl;
import mz.multicore.erp.modules.printing.repository.DocumentPrintControlRepository;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Service
public class SensitiveDocumentPrintService {
    private final DocumentPrintControlRepository repository;
    private final AppUserService appUserService;
    private final AuditLogService auditLogService;

    public SensitiveDocumentPrintService(DocumentPrintControlRepository repository,
                                         AppUserService appUserService,
                                         AuditLogService auditLogService) {
        this.repository = repository;
        this.appUserService = appUserService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public byte[] firstPrint(SensitiveDocumentType type, Long documentId, String terminal,
                             Supplier<byte[]> renderer) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        DocumentPrintControl control = repository
                .findForUpdate(companyId, type, documentId)
                .orElse(null);
        if (control != null && control.getPrintCount() > 0) {
            throw new BusinessRuleException("Este documento já foi impresso. Use Reimprimir e obtenha autorização de outro gerente ou administrador.");
        }
        byte[] pdf = renderer.get();
        if (control == null) {
            control = new DocumentPrintControl();
            control.setCompanyId(companyId);
            control.setDocumentType(type);
            control.setDocumentId(documentId);
            control.setCreatedBy(CurrentUserContext.getUsername());
        }
        control.setPrintCount(1);
        control.setPrintedAt(LocalDateTime.now());
        control.setLastPrintedBy(CurrentUserContext.getUsername());
        control.setLastTerminal(blankToNull(terminal));
        repository.save(control);
        auditLogService.logCurrent("SENSITIVE_DOCUMENT_PRINT",
                type + " " + documentId + " impresso pela primeira vez.");
        return pdf;
    }

    @Transactional
    public byte[] reprint(SensitiveDocumentType type, Long documentId,
                          ReprintAuthorizationRequest request, Supplier<byte[]> renderer) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessRuleException("Indique o motivo da reimpressão.");
        }
        if (request.approverUsername() == null || request.approverPassword() == null) {
            throw new BusinessRuleException("Informe as credenciais do autorizador.");
        }
        if (CurrentUserContext.getUsername().equalsIgnoreCase(request.approverUsername().trim())) {
            throw new BusinessRuleException("A reimpressão exige autorização de outro utilizador.");
        }
        AppUser approver = appUserService.authenticate(
                request.approverUsername().trim(), request.approverPassword());
        var access = approver.findCompanyAccess(companyId);
        boolean authorised = approver.isPlatformAdmin() || access
                .map(companyAccess -> "MANAGER".equalsIgnoreCase(companyAccess.getRole())
                        || "ADMIN".equalsIgnoreCase(companyAccess.getRole()))
                .orElse(false);
        if (!authorised) {
            throw new BusinessRuleException("O autorizador deve ser gerente ou administrador.");
        }

        DocumentPrintControl control = repository
                .findForUpdate(companyId, type, documentId)
                .orElseThrow(() -> new BusinessRuleException("O documento ainda não possui uma primeira impressão registada."));
        int nextCopy = control.getPrintCount() + 1;
        byte[] marked = PdfReprintMarker.mark(renderer.get(), nextCopy);
        control.setPrintCount(nextCopy);
        control.setPrintedAt(LocalDateTime.now());
        control.setLastPrintedBy(CurrentUserContext.getUsername());
        control.setLastAuthorizedBy(approver.getUsername());
        control.setLastReason(request.reason().trim());
        control.setLastTerminal(blankToNull(request.terminalName()));
        repository.save(control);
        auditLogService.logCurrent("SENSITIVE_DOCUMENT_REPRINT",
                type + " " + documentId + " reimpresso; via " + nextCopy
                        + "; autorizado por " + approver.getUsername() + "; motivo: " + request.reason().trim());
        return marked;
    }

    @Transactional(readOnly = true)
    public DocumentPrintStatusDTO status(SensitiveDocumentType type, Long documentId) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        return repository.findByCompanyIdAndDocumentTypeAndDocumentId(companyId, type, documentId)
                .map(this::toDTO).orElseGet(() -> DocumentPrintStatusDTO.neverPrinted(type, documentId));
    }

    @Transactional(readOnly = true)
    public List<DocumentPrintStatusDTO> statuses(SensitiveDocumentType type, List<Long> ids) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (ids == null || ids.isEmpty()) return List.of();
        var byId = repository.findByCompanyIdAndDocumentTypeAndDocumentIdIn(companyId, type, ids).stream()
                .collect(java.util.stream.Collectors.toMap(DocumentPrintControl::getDocumentId, this::toDTO));
        return ids.stream().distinct()
                .map(id -> byId.getOrDefault(id, DocumentPrintStatusDTO.neverPrinted(type, id))).toList();
    }

    private DocumentPrintStatusDTO toDTO(DocumentPrintControl control) {
        return new DocumentPrintStatusDTO(control.getDocumentType(), control.getDocumentId(),
                control.getPrintCount(), control.getPrintedAt(), control.getLastPrintedBy(),
                control.getLastAuthorizedBy(), control.getLastReason());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

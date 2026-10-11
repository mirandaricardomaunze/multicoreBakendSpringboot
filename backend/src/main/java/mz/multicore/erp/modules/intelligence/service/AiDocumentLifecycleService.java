package mz.multicore.erp.modules.intelligence.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.intelligence.dto.*;
import mz.multicore.erp.modules.intelligence.model.AiDocumentStatus;
import mz.multicore.erp.modules.intelligence.model.AiDocumentVersion;
import mz.multicore.erp.modules.intelligence.model.AiManagedDocument;
import mz.multicore.erp.modules.intelligence.repository.AiDocumentVersionRepository;
import mz.multicore.erp.modules.intelligence.repository.AiManagedDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiDocumentLifecycleService {
    private final AiManagedDocumentRepository documentRepository;
    private final AiDocumentVersionRepository versionRepository;
    private final CompanyRepository companyRepository;
    private final AiManagementReportService reportService;
    private final AuditLogService auditLogService;

    public AiDocumentLifecycleService(AiManagedDocumentRepository documentRepository,
                                      AiDocumentVersionRepository versionRepository,
                                      CompanyRepository companyRepository,
                                      AiManagementReportService reportService,
                                      AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.companyRepository = companyRepository;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AiManagedDocumentDTO create(AiProfessionalDocumentRequest request) {
        PermissionGuard.requireManagerOrAdmin("guardar um documento gerado por IA");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa nao encontrada."));
        byte[] pdf = reportService.renderProfessionalDocument(request);
        String username = CurrentUserContext.getUsername();

        AiManagedDocument document = new AiManagedDocument();
        document.setCompany(company);
        document.setDocumentNumber(nextNumber());
        document.setTitle(request.title().trim());
        document.setDocumentType(request.documentType().trim().toUpperCase(Locale.ROOT));
        document.setAuthorUsername(username);
        document.setCreatedBy(username);
        document = documentRepository.save(document);

        saveVersion(document, 1, request, pdf, username);
        audit("AI_DOCUMENT_CREATED", document, "versao=1");
        return toDTO(document);
    }

    @Transactional(readOnly = true)
    public List<AiManagedDocumentDTO> search(String query) {
        PermissionGuard.requireManagerOrAdmin("consultar documentos gerados por IA");
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return documentRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId()).stream()
                .filter(document -> matches(document, normalized))
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<AiDocumentVersionDTO> versions(Long id) {
        PermissionGuard.requireManagerOrAdmin("consultar versoes do documento");
        AiManagedDocument document = requireDocument(id);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId()).stream()
                .map(this::toVersionDTO).toList();
    }

    @Transactional
    public AiManagedDocumentDTO createVersion(Long id, AiProfessionalDocumentRequest request) {
        PermissionGuard.requireManagerOrAdmin("criar uma nova versao do documento");
        AiManagedDocument document = requireDocument(id);
        if (document.getStatus() == AiDocumentStatus.EM_REVISAO || document.getStatus() == AiDocumentStatus.ARQUIVADO) {
            throw new BusinessRuleException("Retire o documento da revisao ou do arquivo antes de criar uma nova versao.");
        }
        byte[] pdf = reportService.renderProfessionalDocument(request);
        int nextVersion = document.getCurrentVersion() + 1;
        saveVersion(document, nextVersion, request, pdf, CurrentUserContext.getUsername());
        document.setTitle(request.title().trim());
        document.setDocumentType(request.documentType().trim().toUpperCase(Locale.ROOT));
        document.setCurrentVersion(nextVersion);
        document.setStatus(AiDocumentStatus.RASCUNHO);
        clearDecision(document);
        documentRepository.save(document);
        audit("AI_DOCUMENT_VERSION_CREATED", document, "versao=" + nextVersion);
        return toDTO(document);
    }

    @Transactional
    public AiManagedDocumentDTO submit(Long id) {
        PermissionGuard.requireManagerOrAdmin("submeter o documento para revisao");
        AiManagedDocument document = requireDocument(id);
        if (document.getStatus() != AiDocumentStatus.RASCUNHO && document.getStatus() != AiDocumentStatus.REJEITADO) {
            throw new BusinessRuleException("Apenas documentos em rascunho ou rejeitados podem ser submetidos.");
        }
        document.setStatus(AiDocumentStatus.EM_REVISAO);
        document.setSubmittedAt(LocalDateTime.now());
        clearDecision(document);
        documentRepository.save(document);
        audit("AI_DOCUMENT_SUBMITTED", document, null);
        return toDTO(document);
    }

    @Transactional
    public AiManagedDocumentDTO approve(Long id) {
        PermissionGuard.requireAdmin("aprovar o documento gerado por IA");
        AiManagedDocument document = requireReviewableByAnotherUser(id);
        document.setStatus(AiDocumentStatus.APROVADO);
        registerDecision(document, null);
        documentRepository.save(document);
        audit("AI_DOCUMENT_APPROVED", document, null);
        return toDTO(document);
    }

    @Transactional
    public AiManagedDocumentDTO reject(Long id, AiDocumentReviewRequest request) {
        PermissionGuard.requireAdmin("rejeitar o documento gerado por IA");
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.length() < 3) {
            throw new BusinessRuleException("Indique o motivo da rejeicao.");
        }
        AiManagedDocument document = requireReviewableByAnotherUser(id);
        document.setStatus(AiDocumentStatus.REJEITADO);
        registerDecision(document, reason);
        documentRepository.save(document);
        audit("AI_DOCUMENT_REJECTED", document, "motivoCaracteres=" + reason.length());
        return toDTO(document);
    }

    @Transactional
    public AiManagedDocumentDTO archive(Long id) {
        PermissionGuard.requireManagerOrAdmin("arquivar o documento");
        AiManagedDocument document = requireDocument(id);
        if (document.getStatus() != AiDocumentStatus.APROVADO && document.getStatus() != AiDocumentStatus.REJEITADO) {
            throw new BusinessRuleException("Apenas documentos aprovados ou rejeitados podem ser arquivados.");
        }
        document.setStatus(AiDocumentStatus.ARQUIVADO);
        document.setArchivedAt(LocalDateTime.now());
        documentRepository.save(document);
        audit("AI_DOCUMENT_ARCHIVED", document, null);
        return toDTO(document);
    }

    @Transactional
    public byte[] pdf(Long id, int versionNumber) {
        PermissionGuard.requireManagerOrAdmin("abrir o PDF do documento");
        AiManagedDocument document = requireDocument(id);
        AiDocumentVersion version = versionRepository.findByDocumentIdAndVersionNumber(document.getId(), versionNumber)
                .orElseThrow(() -> new BusinessRuleException("Versao do documento nao encontrada."));
        audit("AI_DOCUMENT_EXPORTED", document, "versao=" + versionNumber);
        return version.getPdfData();
    }

    private AiManagedDocument requireReviewableByAnotherUser(Long id) {
        AiManagedDocument document = requireDocument(id);
        if (document.getStatus() != AiDocumentStatus.EM_REVISAO) {
            throw new BusinessRuleException("O documento deve estar em revisao para receber uma decisao.");
        }
        if (document.getAuthorUsername().equalsIgnoreCase(CurrentUserContext.getUsername())) {
            throw new BusinessRuleException("O autor nao pode aprovar nem rejeitar o proprio documento.");
        }
        return document;
    }

    private AiManagedDocument requireDocument(Long id) {
        return documentRepository.findByIdAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Documento nao encontrado nesta empresa."));
    }

    private void saveVersion(AiManagedDocument document, int number, AiProfessionalDocumentRequest request,
                             byte[] pdf, String username) {
        AiDocumentVersion version = new AiDocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(number);
        version.setInstructions(request.instructions().trim());
        version.setGeneratedContent(generatedContent(request));
        version.setPdfData(pdf);
        version.setGeneratedBy(username);
        version.setCreatedBy(username);
        versionRepository.save(version);
    }

    private String generatedContent(AiProfessionalDocumentRequest request) {
        if (request.analysis() == null || request.analysis().answer() == null || request.analysis().answer().isBlank()) {
            return request.instructions().trim();
        }
        return request.instructions().trim() + "\n\nAnalise de suporte:\n" + request.analysis().answer().trim();
    }

    private boolean matches(AiManagedDocument document, String query) {
        if (query.isBlank()) return true;
        return document.getTitle().toLowerCase(Locale.ROOT).contains(query)
                || document.getDocumentNumber().toLowerCase(Locale.ROOT).contains(query)
                || document.getDocumentType().toLowerCase(Locale.ROOT).contains(query)
                || document.getStatus().name().toLowerCase(Locale.ROOT).contains(query);
    }

    private void registerDecision(AiManagedDocument document, String rejectionReason) {
        document.setReviewedAt(LocalDateTime.now());
        document.setReviewedBy(CurrentUserContext.getUsername());
        document.setRejectionReason(rejectionReason);
    }

    private void clearDecision(AiManagedDocument document) {
        document.setReviewedAt(null);
        document.setReviewedBy(null);
        document.setRejectionReason(null);
    }

    private String nextNumber() {
        return "IA-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void audit(String action, AiManagedDocument document, String suffix) {
        String details = "documentoId=" + document.getId() + "; numero=" + document.getDocumentNumber()
                + "; tipo=" + document.getDocumentType() + "; estado=" + document.getStatus()
                + (suffix == null ? "" : "; " + suffix);
        auditLogService.logCurrent(action, details);
    }

    private AiManagedDocumentDTO toDTO(AiManagedDocument document) {
        return new AiManagedDocumentDTO(document.getId(), document.getDocumentNumber(), document.getTitle(),
                document.getDocumentType(), document.getStatus().name(), document.getCurrentVersion(),
                document.getAuthorUsername(), document.getCreatedAt(), document.getSubmittedAt(),
                document.getReviewedAt(), document.getReviewedBy(), document.getRejectionReason());
    }

    private AiDocumentVersionDTO toVersionDTO(AiDocumentVersion version) {
        return new AiDocumentVersionDTO(version.getId(), version.getVersionNumber(), version.getInstructions(),
                version.getGeneratedContent(), version.getGeneratedBy(), version.getCreatedAt());
    }
}

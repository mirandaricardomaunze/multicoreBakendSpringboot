package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmployeeDocument;
import mz.multicore.erp.modules.hr.repository.EmployeeDocumentRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Documentos do colaborador com data de validade. Ver docs/RH_COMPLETO_SPEC.md §B8.8.
 *
 * <p><b>O DIRE de um trabalhador estrangeiro caducar sem aviso é multa</b> — e essa data não
 * existia em sítio nenhum do sistema. Não é um arquivo digital (não guarda ficheiros): é o que
 * torna a <b>data</b> conhecida, que é a parte que custa dinheiro quando falha.
 */
@Service
public class EmployeeDocumentService {

    /** Antecedência com que um documento a caducar passa a ser avisado. */
    public static final int EXPIRY_ALERT_DAYS = 45;

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public EmployeeDocumentService(EmployeeDocumentRepository documentRepository,
                                   EmployeeRepository employeeRepository,
                                   CompanyRepository companyRepository,
                                   AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentDTO> list() {
        return documentRepository.findAllByCompany(currentCompanyId()).stream()
                .map(EmployeeDocumentService::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentDTO> listForEmployee(Long employeeId) {
        return documentRepository.findByEmployee(employeeId, currentCompanyId()).stream()
                .map(EmployeeDocumentService::toDTO).toList();
    }

    /** Os que caducam nos próximos {@value #EXPIRY_ALERT_DAYS} dias <b>e os que já caducaram</b>. */
    @Transactional(readOnly = true)
    public List<EmployeeDocumentDTO> expiringSoon() {
        return documentRepository
                .findExpiringUntil(currentCompanyId(), LocalDate.now().plusDays(EXPIRY_ALERT_DAYS))
                .stream().map(EmployeeDocumentService::toDTO).toList();
    }

    @Transactional
    public EmployeeDocumentDTO save(SaveEmployeeDocumentRequest request) {
        ensureHrManager();
        Employee employee = employeeRepository
                .findByIdAndCompanyId(request.employeeId(), currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
        if (request.expiryDate() != null && request.issueDate() != null
                && request.expiryDate().isBefore(request.issueDate())) {
            throw new BusinessRuleException("A validade não pode ser anterior à emissão.");
        }

        EmployeeDocument document = new EmployeeDocument();
        document.setCompany(companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        document.setEmployee(employee);
        document.setDocumentType(request.documentType().trim().toUpperCase());
        document.setDocumentNumber(blankToNull(request.documentNumber()));
        document.setIssueDate(request.issueDate());
        document.setExpiryDate(request.expiryDate());
        document.setNotes(blankToNull(request.notes()));

        EmployeeDocument saved = documentRepository.save(document);
        auditLogService.logCurrent("EMPLOYEE_DOCUMENT_SAVE", String.format(
                "Documento %s de %s registado%s",
                saved.getDocumentType(), employee.getName(),
                saved.getExpiryDate() == null ? " (não caduca)" : ", válido até " + saved.getExpiryDate()));
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        ensureHrManager();
        EmployeeDocument document = documentRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Documento não encontrado."));
        String detail = String.format("Documento %s de %s eliminado",
                document.getDocumentType(), document.getEmployee().getName());
        documentRepository.delete(document);
        auditLogService.logCurrent("EMPLOYEE_DOCUMENT_DELETE", detail);
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem gerir documentos de colaboradores.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static EmployeeDocumentDTO toDTO(EmployeeDocument d) {
        LocalDate today = LocalDate.now();
        return new EmployeeDocumentDTO(d.getId(), d.getEmployee().getId(), d.getEmployee().getName(),
                d.getDocumentType(), d.getDocumentNumber(), d.getIssueDate(), d.getExpiryDate(),
                d.daysUntilExpiry(today), d.isExpired(today), d.getNotes());
    }
}

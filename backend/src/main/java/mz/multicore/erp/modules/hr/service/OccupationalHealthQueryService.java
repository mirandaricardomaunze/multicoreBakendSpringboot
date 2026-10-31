package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.MissingHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthAttachmentDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthSummaryDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.toDTO;
import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.toSummary;

@Service
public class OccupationalHealthQueryService {
    private final OccupationalHealthExamRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AttachmentCrypto attachmentCrypto;
    private final AuditLogService auditLogService;

    public OccupationalHealthQueryService(OccupationalHealthExamRepository repository,
                                          EmployeeRepository employeeRepository,
                                          AttachmentCrypto attachmentCrypto,
                                          AuditLogService auditLogService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.attachmentCrypto = attachmentCrypto;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public OccupationalHealthSummaryDTO summary(Long employeeId) {
        Employee employee = verifyEmployee(employeeId);
        ensureManagerOrSelf(employee, "consultar a aptidão médica de outro trabalhador");
        return repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(companyId(), employeeId)
                .map(OccupationalHealthMapper::toSummary)
                .orElse(new OccupationalHealthSummaryDTO(employeeId, false, null, null, null, null, "NOT_REGISTERED"));
    }

    @Transactional
    public List<OccupationalHealthExamDTO> history(Long employeeId) {
        PermissionGuard.requireManagerOrAdmin("consultar dados de saúde ocupacional");
        Employee employee = verifyEmployee(employeeId);
        List<OccupationalHealthExamDTO> history =
                repository.findHistory(companyId(), employeeId).stream().map(OccupationalHealthMapper::toDTO).toList();
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_ACCESS", String.format(
                "%s consultou o historial de saúde ocupacional de %s (%d registo(s))",
                CurrentUserContext.getUsername(), employee.getName(), history.size()));
        return history;
    }

    @Transactional
    public OccupationalHealthAttachmentDTO attachment(Long examId) {
        PermissionGuard.requireManagerOrAdmin("abrir comprovativos de exames de saúde ocupacional");
        OccupationalHealthExam exam = repository.findByIdAndCompanyId(examId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Exame não encontrado na empresa activa."));
        byte[] stored = exam.getAttachmentData();
        if (stored == null || stored.length == 0) {
            throw new BusinessRuleException("Este exame não tem comprovativo digitalizado.");
        }
        byte[] content = attachmentCrypto.reveal(stored);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_ATTACHMENT_ACCESS", String.format(
                "%s abriu o comprovativo do exame de %s de %s",
                CurrentUserContext.getUsername(), exam.getEmployee().getName(), exam.getExamDate()));
        return new OccupationalHealthAttachmentDTO(exam.getId(),
                exam.getAttachmentName() == null ? "comprovativo" : exam.getAttachmentName(), content);
    }

    @Transactional(readOnly = true)
    public List<OccupationalHealthExamDTO> expiring() {
        PermissionGuard.requireManagerOrAdmin("consultar alertas de saúde ocupacional");
        return repository.findLatestExpiring(companyId(), LocalDate.now().plusDays(OccupationalHealthService.ALERT_DAYS))
                .stream().map(OccupationalHealthMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<MissingHealthExamDTO> missingExams() {
        PermissionGuard.requireManagerOrAdmin("consultar a conformidade de saúde ocupacional");
        Long companyId = companyId();
        Set<Long> withExam = new HashSet<>(repository.findEmployeeIdsWithExam(companyId));
        LocalDate today = LocalDate.now();
        return employeeRepository.findByCompanyIdOrderByName(companyId).stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .filter(employee -> !withExam.contains(employee.getId()))
                .map(employee -> new MissingHealthExamDTO(employee.getId(), employee.getName(),
                        employee.getRole(), employee.getDepartment(), employee.getHireDate(),
                        employee.getHireDate() == null ? null
                                : ChronoUnit.DAYS.between(employee.getHireDate(), today)))
                .sorted(Comparator.comparing(MissingHealthExamDTO::daysSinceHire,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void ensureManagerOrSelf(Employee employee, String operation) {
        if (PermissionGuard.isManagerOrAdmin()) return;
        String username = CurrentUserContext.getUsername();
        Employee self = username == null ? null
                : employeeRepository.findByCompanyIdAndAppUserUsername(companyId(), username).orElse(null);
        if (self == null || !self.getId().equals(employee.getId())) {
            throw new BusinessRuleException(String.format(
                    "Sem permissão para %s. A aptidão médica é dado de saúde: só o próprio "
                            + "trabalhador e os gestores de RH lhe podem aceder.", operation));
        }
    }

    private Employee verifyEmployee(Long employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Trabalhador não encontrado na empresa activa."));
    }

    private Long companyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }
}

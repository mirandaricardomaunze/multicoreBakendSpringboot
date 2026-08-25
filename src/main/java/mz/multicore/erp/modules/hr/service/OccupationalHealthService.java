package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class OccupationalHealthService {
    public static final int ALERT_DAYS = 60;
    private static final Set<String> RESULTS = Set.of("FIT", "FIT_WITH_RESTRICTIONS", "UNFIT");

    private final OccupationalHealthExamRepository repository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public OccupationalHealthService(OccupationalHealthExamRepository repository,
                                     EmployeeRepository employeeRepository,
                                     CompanyRepository companyRepository,
                                     AuditLogService auditLogService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public OccupationalHealthSummaryDTO summary(Long employeeId) {
        verifyEmployee(employeeId);
        return repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(companyId(), employeeId)
                .map(this::toSummary)
                .orElse(new OccupationalHealthSummaryDTO(employeeId, false, null, null, null, null, "NOT_REGISTERED"));
    }

    @Transactional(readOnly = true)
    public List<OccupationalHealthExamDTO> history(Long employeeId) {
        PermissionGuard.requireManagerOrAdmin("consultar dados de saúde ocupacional");
        verifyEmployee(employeeId);
        return repository.findHistory(companyId(), employeeId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<OccupationalHealthExamDTO> expiring() {
        PermissionGuard.requireManagerOrAdmin("consultar alertas de saúde ocupacional");
        return repository.findLatestExpiring(companyId(), LocalDate.now().plusDays(ALERT_DAYS))
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public OccupationalHealthExamDTO register(SaveOccupationalHealthExamRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar exames de saúde ocupacional");
        String result = request.fitnessResult().trim().toUpperCase();
        if (!RESULTS.contains(result)) throw new BusinessRuleException("Resultado de aptidão inválido.");
        if (request.expiryDate().isBefore(request.examDate())) {
            throw new BusinessRuleException("A validade não pode ser anterior à data do exame.");
        }
        if ("FIT_WITH_RESTRICTIONS".equals(result)
                && (request.restrictions() == null || request.restrictions().isBlank())) {
            throw new BusinessRuleException("Indique as restrições aplicáveis ao trabalhador.");
        }
        var employee = verifyEmployee(request.employeeId());
        OccupationalHealthExam exam = new OccupationalHealthExam();
        exam.setCompany(companyRepository.findById(companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        exam.setEmployee(employee);
        exam.setCardNumber(blank(request.cardNumber()));
        exam.setExamDate(request.examDate());
        exam.setExpiryDate(request.expiryDate());
        exam.setFitnessResult(result);
        exam.setClinic(blank(request.clinic()));
        exam.setDoctorName(blank(request.doctorName()));
        exam.setRestrictions(blank(request.restrictions()));
        exam.setNotes(blank(request.notes()));
        exam.setAttachmentName(blank(request.attachmentName()));
        exam.setAttachmentData(request.attachmentData());
        OccupationalHealthExam saved = repository.save(exam);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_EXAM_REGISTER", String.format(
                "Exame ocupacional de %s registado: %s, válido até %s",
                employee.getName(), result, saved.getExpiryDate()));
        return toDTO(saved);
    }

    private mz.multicore.erp.modules.hr.model.Employee verifyEmployee(Long employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Trabalhador não encontrado na empresa activa."));
    }

    private Long companyId() { return CurrentUserContext.requireCurrentCompanyId(); }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String status(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return days < 0 ? "EXPIRED" : days <= ALERT_DAYS ? "EXPIRING" : "VALID";
    }

    private OccupationalHealthSummaryDTO toSummary(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return new OccupationalHealthSummaryDTO(exam.getEmployee().getId(), true,
                exam.getFitnessResult(), exam.getExamDate(), exam.getExpiryDate(), days, status(exam));
    }

    private OccupationalHealthExamDTO toDTO(OccupationalHealthExam exam) {
        return new OccupationalHealthExamDTO(exam.getId(), exam.getEmployee().getId(),
                exam.getEmployee().getName(), exam.getCardNumber(), exam.getExamDate(), exam.getExpiryDate(),
                exam.getFitnessResult(), exam.getClinic(), exam.getDoctorName(), exam.getRestrictions(),
                exam.getNotes(), exam.getAttachmentData() != null && exam.getAttachmentData().length > 0,
                exam.getAttachmentName(), exam.daysUntilExpiry(LocalDate.now()), status(exam));
    }
}

package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HrAbsenceAndVacationService {

    private static final int FALLBACK_ANNUAL_VACATION_DAYS = 22;

    private final AbsenceRepository absenceRepository;
    private final VacationRepository vacationRepository;
    private final HrAccessGuard guard;
    private final AuditLogService auditLogService;
    private final TimeSheetService timeSheetService;
    private final HrPolicyService hrPolicyService;

    public HrAbsenceAndVacationService(
            AbsenceRepository absenceRepository,
            VacationRepository vacationRepository,
            HrAccessGuard guard,
            AuditLogService auditLogService,
            @Lazy TimeSheetService timeSheetService,
            HrPolicyService hrPolicyService
    ) {
        this.absenceRepository = absenceRepository;
        this.vacationRepository = vacationRepository;
        this.guard = guard;
        this.auditLogService = auditLogService;
        this.timeSheetService = timeSheetService;
        this.hrPolicyService = hrPolicyService;
    }

    public AbsenceDTO recordAbsence(CreateAbsenceRequest request) {
        guard.ensureHrManager();
        Employee employee = guard.findActiveEmployee(request.employeeId());
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        Absence a = new Absence();
        a.setEmployee(employee);
        a.setAbsenceType(request.absenceType());
        a.setStartDate(request.startDate());
        a.setEndDate(request.endDate());
        a.setTotalDays((int) (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1));
        a.setReason(request.reason());
        a.setHasSupportingDocument(request.hasSupportingDocument());

        Absence saved = absenceRepository.save(a);
        auditLogService.logCurrent("ABSENCE_CREATE", String.format(
                "Falta %s de %s registada de %s a %s (%d dia(s))",
                saved.getAbsenceType(), employee.getName(),
                saved.getStartDate(), saved.getEndDate(), saved.getTotalDays()));
        return absenceToDTO(saved);
    }

    public AbsenceDTO justifyAbsence(Long id, String absenceType, String reason, boolean hasDocument) {
        guard.ensureHrManager();
        if (absenceType == null || absenceType.isBlank()) {
            throw new BusinessRuleException("Indique o tipo de falta.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Justificar uma falta exige um motivo.");
        }
        Absence absence = absenceRepository.findByIdAndEmployeeCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Falta não encontrada."));

        String previousType = absence.getAbsenceType();
        absence.setAbsenceType(absenceType.trim().toUpperCase());
        absence.setReason(reason.trim());
        absence.setHasSupportingDocument(hasDocument);

        Absence saved = absenceRepository.save(absence);
        auditLogService.logCurrent("ABSENCE_JUSTIFY", String.format(
                "Falta de %s em %s: %s → %s. Motivo: %s%s",
                saved.getEmployee().getName(), saved.getStartDate(), previousType,
                saved.getAbsenceType(), saved.getReason(),
                hasDocument ? " (com documento)" : ""));
        return absenceToDTO(saved);
    }

    public void deleteAbsence(Long id) {
        guard.ensureHrManager();
        Long companyId = guard.currentCompanyId();
        Absence absence = absenceRepository.findByIdAndEmployeeCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Falta não encontrada."));
        String detail = String.format(
                "Falta %s de %s (%s a %s, %d dia(s)) eliminada",
                absence.getAbsenceType(), absence.getEmployee().getName(),
                absence.getStartDate(), absence.getEndDate(), absence.getTotalDays());

        absenceRepository.deleteByIdAndEmployeeCompanyId(id, companyId);
        auditLogService.logCurrent("ABSENCE_DELETE", detail);
    }

    public List<AbsenceDTO> getAllAbsences() {
        return absenceRepository.findAllWithEmployeeByCompanyId(guard.currentCompanyId()).stream()
                .map(this::absenceToDTO).collect(Collectors.toList());
    }

    public VacationDTO submitVacation(CreateVacationRequest request) {
        Employee employee = guard.findActiveEmployee(request.employeeId());
        guard.ensureCanActFor(employee);
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        int requestedDays = timeSheetService.workingDaysBetween(request.startDate(), request.endDate());
        if (requestedDays <= 0) {
            throw new BusinessRuleException(
                    "O período pedido não tem nenhum dia útil segundo o horário da empresa.");
        }
        int reserved = vacationRepository.sumReservedDays(employee.getId(), request.yearReference());
        int entitlement = annualVacationDays(employee, request.startDate());
        int remaining = entitlement - reserved;
        if (requestedDays > remaining) {
            throw new BusinessRuleException(String.format(
                    "Saldo de férias insuficiente para %d: pedido de %d dia(s) útil(eis), disponível %d "
                            + "(direito anual %d, já reservados %d).",
                    request.yearReference(), requestedDays, Math.max(remaining, 0),
                    entitlement, reserved));
        }

        Vacation v = new Vacation();
        v.setEmployee(employee);
        v.setStartDate(request.startDate());
        v.setEndDate(request.endDate());
        v.setTotalDays(requestedDays);
        v.setYearReference(request.yearReference());
        v.setStatus("PENDING");
        v.setNotes(request.notes());
        return vacationToDTO(vacationRepository.save(v));
    }

    public VacationDTO decideVacation(Long id, boolean approve, String rejectionReason) {
        guard.ensureHrManager();
        Vacation v = vacationRepository.findByIdAndEmployeeCompanyId(id, guard.currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"PENDING".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas pedidos pendentes podem ser decididos.");
        }
        if (!approve && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new BusinessRuleException("Indique o motivo da rejeição das férias.");
        }
        String decidedBy = CurrentUserContext.getUsername();
        v.setStatus(approve ? "APPROVED" : "REJECTED");
        v.setDecisionBy(decidedBy);
        v.setDecisionAt(LocalDateTime.now());
        if (!approve) {
            v.setRejectionReason(rejectionReason);
        }
        Vacation saved = vacationRepository.save(v);
        auditLogService.logCurrent("VACATION_DECISION", String.format(
                "Férias de %s %s por %s%s",
                saved.getEmployee().getName(), approve ? "aprovadas" : "rejeitadas", decidedBy,
                approve ? "" : " (motivo: " + rejectionReason + ")"));
        return vacationToDTO(saved);
    }

    public List<VacationDTO> getAllVacations() {
        return vacationRepository.findAllWithEmployeeByCompanyId(guard.currentCompanyId()).stream()
                .map(this::vacationToDTO).collect(Collectors.toList());
    }

    private int annualVacationDays(Employee employee, LocalDate referenceDate) {
        LocalDate hireDate = employee.getHireDate();
        int completedYears = hireDate == null || referenceDate.isBefore(hireDate)
                ? 0
                : (int) ChronoUnit.YEARS.between(hireDate, referenceDate);
        return hrPolicyService.annualVacationDays(completedYears, referenceDate)
                .orElse(FALLBACK_ANNUAL_VACATION_DAYS);
    }

    private AbsenceDTO absenceToDTO(Absence a) {
        return new AbsenceDTO(
                a.getId(), a.getEmployee().getId(), a.getEmployee().getName(),
                a.getAbsenceType(), a.getStartDate(), a.getEndDate(),
                a.getTotalDays(), a.getReason(), a.isHasSupportingDocument()
        );
    }

    private VacationDTO vacationToDTO(Vacation v) {
        return new VacationDTO(
                v.getId(), v.getEmployee().getId(), v.getEmployee().getName(),
                v.getStartDate(), v.getEndDate(), v.getTotalDays(),
                v.getYearReference(), v.getStatus(), v.getNotes(),
                v.getDecisionBy(), v.getDecisionAt(), v.getRejectionReason()
        );
    }
}

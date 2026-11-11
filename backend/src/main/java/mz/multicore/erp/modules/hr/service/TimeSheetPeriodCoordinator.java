package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.TimeSheet;
import mz.multicore.erp.modules.hr.model.TimeSheetStatus;
import mz.multicore.erp.modules.hr.model.WorkSchedule;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TimeEntryRepository;
import mz.multicore.erp.modules.hr.repository.TimeSheetRepository;
import mz.multicore.erp.modules.hr.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fecho, trancamento e reabertura auditada de períodos de folha de ponto, com geração de faltas automáticas.
 */
@Service
public class TimeSheetPeriodCoordinator {

    public static final String PENDING_JUSTIFICATION = "PENDING_JUSTIFICATION";

    private final TimeSheetRepository timeSheetRepository;
    private final CompanyLookup companyLookup;
    private final EmployeeRepository employeeRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AbsenceRepository absenceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AuditLogService auditLogService;
    private final TimeSheetCalculator calculator;

    public TimeSheetPeriodCoordinator(TimeSheetRepository timeSheetRepository,
                                      CompanyLookup companyLookup,
                                      EmployeeRepository employeeRepository,
                                      TimeEntryRepository timeEntryRepository,
                                      AbsenceRepository absenceRepository,
                                      WorkScheduleRepository workScheduleRepository,
                                      AuditLogService auditLogService,
                                      TimeSheetCalculator calculator) {
        this.timeSheetRepository = timeSheetRepository;
        this.companyLookup = companyLookup;
        this.employeeRepository = employeeRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.absenceRepository = absenceRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.auditLogService = auditLogService;
        this.calculator = calculator;
    }

    @Transactional
    public TimeSheetDTO closePeriod(int year, int month) {
        PermissionGuard.requireManagerOrAdmin("gerir o ponto");
        TimeSheet sheet = findOrCreateSheet(year, month);
        if (sheet.isClosed()) {
            throw new BusinessRuleException(
                    String.format("A folha de ponto de %d/%d já está fechada.", month, year));
        }
        sheet.setStatus(TimeSheetStatus.FECHADA);
        sheet.setClosedBy(CurrentUserContext.getUsername());
        sheet.setClosedAt(LocalDateTime.now());
        timeSheetRepository.save(sheet);

        int generated = generateMissingDayAbsences(year, month);
        auditLogService.logCurrent("TIMESHEET_CLOSE", String.format(
                "Folha de ponto %d/%d fechada: %d falta(s) por justificar geradas", month, year, generated));
        return calculator.getMonthlySheet(year, month);
    }

    public int generateMissingDayAbsences(int year, int month) {
        Long companyId = currentCompanyId();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = endOfMonth(from);
        WorkSchedule schedule = defaultSchedule(companyId);

        int created = 0;
        for (TimeSheetDTO.TimeSheetLineDTO line : calculator.getMonthlySheet(year, month).lines()) {
            if (line.missingDays() == 0) {
                continue;
            }
            Employee employee = employeeRepository.findByIdAndCompanyId(line.employeeId(), companyId)
                    .orElse(null);
            if (employee == null) {
                continue;
            }
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                if (schedule.isRestDay(day.getDayOfWeek())) {
                    continue;
                }
                if (timeEntryRepository.existsByCompanyIdAndEmployeeIdAndEntryDate(
                        companyId, employee.getId(), day)) {
                    continue;
                }
                if (absenceRepository.existsByEmployeeIdAndStartDate(employee.getId(), day)) {
                    continue;
                }
                Absence absence = new Absence();
                absence.setEmployee(employee);
                absence.setAbsenceType(PENDING_JUSTIFICATION);
                absence.setStartDate(day);
                absence.setEndDate(day);
                absence.setTotalDays(1);
                absence.setReason("Dia previsto sem marcação de ponto");
                absence.setHasSupportingDocument(false);
                absenceRepository.save(absence);
                created++;
            }
        }
        return created;
    }

    @Transactional
    public TimeSheetDTO reopenPeriod(int year, int month, String reason) {
        PermissionGuard.requireManagerOrAdmin("gerir o ponto");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Reabrir a folha de ponto exige um motivo.");
        }
        TimeSheet sheet = timeSheetRepository.findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month)
                .orElseThrow(() -> new BusinessRuleException(
                        String.format("A folha de ponto de %d/%d nunca foi fechada.", month, year)));
        if (!sheet.isClosed()) {
            throw new BusinessRuleException(
                    String.format("A folha de ponto de %d/%d já está aberta.", month, year));
        }
        sheet.setStatus(TimeSheetStatus.ABERTA);
        sheet.setReopenReason(reason.trim());
        timeSheetRepository.save(sheet);
        auditLogService.logCurrent("TIMESHEET_REOPEN", String.format(
                "Folha de ponto %d/%d reaberta: %s", month, year, reason.trim()));
        return calculator.getMonthlySheet(year, month);
    }

    @Transactional(readOnly = true)
    public boolean isPeriodClosed(int year, int month) {
        return timeSheetRepository.findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month)
                .map(TimeSheet::isClosed).orElse(false);
    }

    public void ensurePeriodOpen(int year, int month) {
        if (isPeriodClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "A folha de ponto de %d/%d está fechada. Reabra-a (com motivo) para a alterar.",
                    month, year));
        }
    }

    public TimeSheet findOrCreateSheet(int year, int month) {
        Long companyId = currentCompanyId();
        return timeSheetRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .orElseGet(() -> {
                    TimeSheet sheet = new TimeSheet();
                    sheet.setCompany(currentCompany());
                    sheet.setYear(year);
                    sheet.setMonth(month);
                    return sheet;
                });
    }

    private WorkSchedule defaultSchedule(Long companyId) {
        return workScheduleRepository.findByCompanyIdOrderByName(companyId).stream().findFirst()
                .orElseGet(WorkSchedule::new);
    }

    private Company currentCompany() {
        return companyLookup.requireActive(currentCompanyId());
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private static LocalDate endOfMonth(LocalDate monthStart) {
        return monthStart.withDayOfMonth(monthStart.lengthOfMonth());
    }
}

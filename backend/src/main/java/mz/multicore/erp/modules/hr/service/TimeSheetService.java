package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TimeEntryRepository;
import mz.multicore.erp.modules.hr.repository.TimeSheetRepository;
import mz.multicore.erp.modules.hr.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Ponto e assiduidade. Ver docs/RH_COMPLETO_SPEC.md §B2.
 * Orquestrador modular delegando para colaboradores especializados.
 */
@Service
public class TimeSheetService {

    /** Falta nascida do ponto e ainda por explicar. Não desconta — só as UNJUSTIFIED descontam. */
    public static final String PENDING_JUSTIFICATION = "PENDING_JUSTIFICATION";

    private final TimeEntryRegistrationService registrationService;
    private final TimeSheetCalculator calculator;
    private final TimeSheetPeriodCoordinator periodCoordinator;

    public TimeSheetService(TimeEntryRegistrationService registrationService,
                            TimeSheetCalculator calculator,
                            TimeSheetPeriodCoordinator periodCoordinator) {
        this.registrationService = registrationService;
        this.calculator = calculator;
        this.periodCoordinator = periodCoordinator;
    }

    /** Construtor para retrocompatibilidade com testes legados. */
    public TimeSheetService(TimeEntryRepository timeEntryRepository,
                            TimeSheetRepository timeSheetRepository,
                            WorkScheduleRepository workScheduleRepository,
                            EmployeeRepository employeeRepository,
                            CompanyRepository companyRepository,
                            EmploymentContractService contractService,
                            AbsenceRepository absenceRepository,
                            AuditLogService auditLogService) {
        TimeSheetCalculator calc = new TimeSheetCalculator(timeEntryRepository, timeSheetRepository,
                workScheduleRepository, employeeRepository, contractService);
        TimeSheetPeriodCoordinator coord = new TimeSheetPeriodCoordinator(timeSheetRepository, companyRepository,
                employeeRepository, timeEntryRepository, absenceRepository, workScheduleRepository,
                auditLogService, calc);
        TimeEntryRegistrationService reg = new TimeEntryRegistrationService(timeEntryRepository, employeeRepository,
                companyRepository, auditLogService, coord);
        this.registrationService = reg;
        this.calculator = calc;
        this.periodCoordinator = coord;
    }

    // ─── Marcações ────────────────────────────────────────────────────────────

    @Transactional
    public TimeEntryDTO recordEntry(CreateTimeEntryRequest request) {
        return registrationService.recordEntry(request);
    }

    @Transactional
    public void deleteEntry(Long id) {
        registrationService.deleteEntry(id);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getEntries(int year, int month) {
        return registrationService.getEntries(year, month);
    }

    // ─── Folha de ponto do mês ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TimeSheetDTO getMonthlySheet(int year, int month) {
        return calculator.getMonthlySheet(year, month);
    }

    @Transactional(readOnly = true)
    public Optional<TimeSheetDTO.TimeSheetLineDTO> findEmployeeLine(Long employeeId, int year, int month) {
        return calculator.findEmployeeLine(employeeId, year, month);
    }

    @Transactional(readOnly = true)
    public int workingDaysBetween(LocalDate from, LocalDate to) {
        return calculator.workingDaysBetween(from, to);
    }

    // ─── Fecho e reabertura ───────────────────────────────────────────────────

    @Transactional
    public TimeSheetDTO closePeriod(int year, int month) {
        return periodCoordinator.closePeriod(year, month);
    }

    @Transactional
    public TimeSheetDTO reopenPeriod(int year, int month, String reason) {
        return periodCoordinator.reopenPeriod(year, month, reason);
    }

    @Transactional(readOnly = true)
    public boolean isPeriodClosed(int year, int month) {
        return periodCoordinator.isPeriodClosed(year, month);
    }
}

package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.TimeEntry;
import mz.multicore.erp.modules.hr.model.TimeEntrySource;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Registo, remoção e validação de marcações individuais de ponto (check-in/check-out).
 */
@Service
public class TimeEntryRegistrationService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final TimeSheetPeriodCoordinator periodCoordinator;

    public TimeEntryRegistrationService(TimeEntryRepository timeEntryRepository,
                                        EmployeeRepository employeeRepository,
                                        CompanyRepository companyRepository,
                                        AuditLogService auditLogService,
                                        TimeSheetPeriodCoordinator periodCoordinator) {
        this.timeEntryRepository = timeEntryRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
        this.periodCoordinator = periodCoordinator;
    }

    @Transactional
    public TimeEntryDTO recordEntry(CreateTimeEntryRequest request) {
        PermissionGuard.requireManagerOrAdmin("gerir o ponto");
        Long companyId = currentCompanyId();
        Employee employee = findEmployee(request.employeeId());
        periodCoordinator.ensurePeriodOpen(request.entryDate().getYear(), request.entryDate().getMonthValue());

        if (timeEntryRepository.existsByCompanyIdAndEmployeeIdAndEntryDate(
                companyId, employee.getId(), request.entryDate())) {
            throw new BusinessRuleException(String.format(
                    "%s já tem marcação em %s. Elimine a existente antes de registar outra.",
                    employee.getName(), request.entryDate()));
        }

        TimeEntry entry = new TimeEntry();
        entry.setEmployee(employee);
        entry.setCompany(currentCompany());
        entry.setEntryDate(request.entryDate());
        entry.setCheckIn(request.checkIn());
        entry.setCheckOut(request.checkOut());
        entry.setBreakMinutes(Math.max(0, request.breakMinutes()));
        entry.setSource(parseSource(request.source()));
        entry.setRecordedBy(CurrentUserContext.getUsername());
        entry.setNotes(blankToNull(request.notes()));
        validateEntry(entry);

        TimeEntry saved = timeEntryRepository.save(entry);
        auditLogService.logCurrent("TIME_ENTRY_CREATE", String.format(
                "Marcação de %s em %s: %s–%s (%s h), origem %s",
                employee.getName(), saved.getEntryDate(), saved.getCheckIn(), saved.getCheckOut(),
                saved.workedHours(), saved.getSource().getLabel()));
        return toDTO(saved);
    }

    @Transactional
    public void deleteEntry(Long id) {
        PermissionGuard.requireManagerOrAdmin("gerir o ponto");
        Long companyId = currentCompanyId();
        TimeEntry entry = timeEntryRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Marcação não encontrada."));
        periodCoordinator.ensurePeriodOpen(entry.getEntryDate().getYear(), entry.getEntryDate().getMonthValue());

        String detail = String.format("Marcação de %s em %s (%s–%s) eliminada",
                entry.getEmployee().getName(), entry.getEntryDate(),
                entry.getCheckIn(), entry.getCheckOut());
        timeEntryRepository.delete(entry);
        auditLogService.logCurrent("TIME_ENTRY_DELETE", detail);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getEntries(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return timeEntryRepository.findByPeriod(currentCompanyId(), from, to).stream()
                .map(this::toDTO).toList();
    }

    private void validateEntry(TimeEntry entry) {
        if (entry.getCheckIn().equals(entry.getCheckOut())) {
            throw new BusinessRuleException("A entrada e a saída não podem ser à mesma hora.");
        }
        if (entry.workedHours().signum() <= 0) {
            throw new BusinessRuleException(
                    "A pausa é igual ou superior ao tempo entre a entrada e a saída.");
        }
    }

    private Employee findEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessRuleException("Não é possível marcar ponto a um colaborador inactivo.");
        }
        return employee;
    }

    private Company currentCompany() {
        return companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada."));
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private TimeEntrySource parseSource(String raw) {
        if (raw == null || raw.isBlank()) {
            return TimeEntrySource.MANUAL;
        }
        try {
            return TimeEntrySource.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Origem de marcação desconhecida: " + raw);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public TimeEntryDTO toDTO(TimeEntry entry) {
        return new TimeEntryDTO(
                entry.getId(), entry.getEmployee().getId(), entry.getEmployee().getName(),
                entry.getEntryDate(), entry.getCheckIn(), entry.getCheckOut(), entry.getBreakMinutes(),
                entry.workedHours(), entry.crossesMidnight(),
                entry.getSource().name(), entry.getSource().getLabel(),
                entry.getRecordedBy(), entry.getNotes());
    }
}

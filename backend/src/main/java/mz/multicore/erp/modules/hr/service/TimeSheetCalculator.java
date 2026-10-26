package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.TimeEntry;
import mz.multicore.erp.modules.hr.model.TimeSheet;
import mz.multicore.erp.modules.hr.model.TimeSheetStatus;
import mz.multicore.erp.modules.hr.model.WorkSchedule;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TimeEntryRepository;
import mz.multicore.erp.modules.hr.repository.TimeSheetRepository;
import mz.multicore.erp.modules.hr.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Apuração e cálculo mensal de folha de ponto, horas normais, escalões extraordinários e atrasos.
 */
@Service
public class TimeSheetCalculator {

    private final TimeEntryRepository timeEntryRepository;
    private final TimeSheetRepository timeSheetRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentContractService contractService;

    public TimeSheetCalculator(TimeEntryRepository timeEntryRepository,
                               TimeSheetRepository timeSheetRepository,
                               WorkScheduleRepository workScheduleRepository,
                               EmployeeRepository employeeRepository,
                               EmploymentContractService contractService) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeSheetRepository = timeSheetRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.employeeRepository = employeeRepository;
        this.contractService = contractService;
    }

    @Transactional(readOnly = true)
    public TimeSheetDTO getMonthlySheet(int year, int month) {
        Long companyId = currentCompanyId();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = endOfMonth(from);
        WorkSchedule schedule = defaultSchedule(companyId);

        Map<Long, List<TimeEntry>> byEmployee = new LinkedHashMap<>();
        for (TimeEntry entry : timeEntryRepository.findByPeriod(companyId, from, to)) {
            byEmployee.computeIfAbsent(entry.getEmployee().getId(), k -> new ArrayList<>()).add(entry);
        }

        List<TimeSheetDTO.TimeSheetLineDTO> lines = new ArrayList<>();
        for (Employee employee : employeeRepository.findByCompanyIdOrderByName(companyId)) {
            if (!"ACTIVE".equals(employee.getStatus())) {
                continue;
            }
            if (contractService.findContractInPeriod(employee.getId(), from, to).isEmpty()) {
                continue;
            }
            lines.add(computeLine(employee, byEmployee.getOrDefault(employee.getId(), List.of()),
                    schedule, from, to));
        }

        TimeSheetStatus status = timeSheetRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .map(TimeSheet::getStatus).orElse(TimeSheetStatus.ABERTA);
        String closedBy = timeSheetRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .map(TimeSheet::getClosedBy).orElse(null);
        return new TimeSheetDTO(year, month, status.name(), status.getLabel(),
                status.isClosed(), closedBy, lines);
    }

    @Transactional(readOnly = true)
    public Optional<TimeSheetDTO.TimeSheetLineDTO> findEmployeeLine(Long employeeId, int year, int month) {
        return getMonthlySheet(year, month).lines().stream()
                .filter(line -> line.employeeId().equals(employeeId))
                .findFirst();
    }

    public TimeSheetDTO.TimeSheetLineDTO computeLine(Employee employee, List<TimeEntry> entries,
                                                     WorkSchedule schedule, LocalDate from, LocalDate to) {
        Map<LocalDate, TimeEntry> byDate = new LinkedHashMap<>();
        for (TimeEntry entry : entries) {
            byDate.put(entry.getEntryDate(), entry);
        }

        int expectedDays = 0;
        int workedDays = 0;
        int missingDays = 0;
        int lateArrivals = 0;
        BigDecimal expectedHours = BigDecimal.ZERO;
        BigDecimal workedHours = BigDecimal.ZERO;
        BigDecimal normalHours = BigDecimal.ZERO;
        BigDecimal overtimeDay = BigDecimal.ZERO;
        BigDecimal overtimeNight = BigDecimal.ZERO;
        BigDecimal restDayHours = BigDecimal.ZERO;

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            BigDecimal dayExpected = schedule.expectedHours(day.getDayOfWeek());
            boolean restDay = schedule.isRestDay(day.getDayOfWeek());
            if (!restDay) {
                expectedDays++;
                expectedHours = expectedHours.add(dayExpected);
            }

            TimeEntry entry = byDate.get(day);
            if (entry == null) {
                if (!restDay) {
                    missingDays++;
                }
                continue;
            }

            workedDays++;
            BigDecimal worked = entry.workedHours();
            workedHours = workedHours.add(worked);

            if (restDay) {
                restDayHours = restDayHours.add(worked);
                continue;
            }

            BigDecimal normal = worked.min(dayExpected);
            normalHours = normalHours.add(normal);
            BigDecimal extra = worked.subtract(normal);
            if (extra.signum() > 0) {
                if (schedule.isNightHour(entry.getCheckOut())) {
                    overtimeNight = overtimeNight.add(extra);
                } else {
                    overtimeDay = overtimeDay.add(extra);
                }
            }
            if (isLate(entry, schedule)) {
                lateArrivals++;
            }
        }

        return new TimeSheetDTO.TimeSheetLineDTO(
                employee.getId(), employee.getName(), expectedDays, workedDays, missingDays,
                scale(expectedHours), scale(workedHours), scale(normalHours),
                scale(overtimeDay), scale(overtimeNight), scale(restDayHours), lateArrivals);
    }

    public boolean isLate(TimeEntry entry, WorkSchedule schedule) {
        LocalTime limit = schedule.getExpectedStartTime().plusMinutes(schedule.getLateToleranceMinutes());
        return entry.getCheckIn().isAfter(limit);
    }

    @Transactional(readOnly = true)
    public int workingDaysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            return 0;
        }
        WorkSchedule schedule = defaultSchedule(CurrentUserContext.requireCurrentCompanyId());
        int days = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (!schedule.isRestDay(day.getDayOfWeek())) {
                days++;
            }
        }
        return days;
    }

    public WorkSchedule defaultSchedule(Long companyId) {
        return workScheduleRepository.findByCompanyIdOrderByName(companyId).stream().findFirst()
                .orElseGet(WorkSchedule::new);
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private static LocalDate endOfMonth(LocalDate monthStart) {
        return monthStart.withDayOfMonth(monthStart.lengthOfMonth());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

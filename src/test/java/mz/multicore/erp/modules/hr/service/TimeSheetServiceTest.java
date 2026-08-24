package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.TimeEntry;
import mz.multicore.erp.modules.hr.model.TimeSheet;
import mz.multicore.erp.modules.hr.model.TimeSheetStatus;
import mz.multicore.erp.modules.hr.model.WorkSchedule;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TimeEntryRepository;
import mz.multicore.erp.modules.hr.repository.TimeSheetRepository;
import mz.multicore.erp.modules.hr.repository.WorkScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B2 do harness do RH (RHC-20..29). O que está sob teste é o <b>apuramento</b>: é dele que sai o
 * número que vai parar ao recibo, e é ele que hoje não existe (o campo é digitado à mão).
 */
class TimeSheetServiceTest {

    private static final Long COMPANY = 7L;
    /** Junho de 2026: começa a uma segunda-feira, o que torna as contas verificáveis à mão. */
    private static final int YEAR = 2026;
    private static final int MONTH = 6;

    private TimeEntryRepository timeEntryRepository;
    private TimeSheetRepository timeSheetRepository;
    private WorkScheduleRepository workScheduleRepository;
    private EmployeeRepository employeeRepository;
    private CompanyRepository companyRepository;
    private EmploymentContractService contractService;
    private AbsenceRepository absenceRepository;
    private AuditLogService auditLogService;
    private TimeSheetService service;

    @BeforeEach
    void setUp() {
        timeEntryRepository = mock(TimeEntryRepository.class);
        timeSheetRepository = mock(TimeSheetRepository.class);
        workScheduleRepository = mock(WorkScheduleRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        companyRepository = mock(CompanyRepository.class);
        contractService = mock(EmploymentContractService.class);
        absenceRepository = mock(AbsenceRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new TimeSheetService(timeEntryRepository, timeSheetRepository, workScheduleRepository,
                employeeRepository, companyRepository, contractService, absenceRepository, auditLogService);

        Company company = new Company();
        company.setId(COMPANY);
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company));
        when(employeeRepository.findByIdAndCompanyId(5L, COMPANY)).thenReturn(Optional.of(employee()));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timeSheetRepository.save(any(TimeSheet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workScheduleRepository.findByCompanyIdOrderByName(COMPANY)).thenReturn(List.of(schedule()));
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ─── RHC-20/21/22: registar marcações ─────────────────────────────────────

    @Test
    void recordEntry_storesSourceAndAuthor() { // RHC-20
        var dto = service.recordEntry(request(LocalDate.of(YEAR, MONTH, 1),
                LocalTime.of(8, 0), LocalTime.of(17, 0), 60));

        assertEquals(0, new BigDecimal("8.00").compareTo(dto.workedHours()), "9h menos 1h de pausa");
        assertEquals("MANUAL", dto.source());
        assertEquals("gestor", dto.recordedBy(), "uma marcação manual só tem quem a escreveu a responder por ela");
        verify(auditLogService).logCurrent(eq("TIME_ENTRY_CREATE"), any());
    }

    @Test
    void recordEntry_secondEntryForSameDay_isBlocked() { // RHC-21
        when(timeEntryRepository.existsByCompanyIdAndEmployeeIdAndEntryDate(COMPANY, 5L, LocalDate.of(YEAR, MONTH, 1)))
                .thenReturn(true);

        var ex = assertThrows(BusinessRuleException.class, () -> service.recordEntry(
                request(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(17, 0), 0)));
        assertEquals(true, ex.getMessage().contains("já tem marcação"));
        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void recordEntry_nightShiftCrossingMidnight_countsEightHoursNotSixteen() { // RHC-22
        // Quem entra às 22:00 e sai às 06:00 trabalhou oito horas. Tratar a saída "anterior" como
        // erro perderia o turno da noite inteiro.
        var dto = service.recordEntry(request(LocalDate.of(YEAR, MONTH, 1),
                LocalTime.of(22, 0), LocalTime.of(6, 0), 0));

        assertEquals(0, new BigDecimal("8.00").compareTo(dto.workedHours()));
        assertEquals(true, dto.crossesMidnight());
    }

    @Test
    void recordEntry_breakSwallowsTheWholeShift_isBlocked() {
        var ex = assertThrows(BusinessRuleException.class, () -> service.recordEntry(
                request(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(9, 0), 60)));
        assertEquals(true, ex.getMessage().contains("pausa"));
    }

    @Test
    void recordEntry_employeeRole_isBlocked() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.recordEntry(
                request(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(17, 0), 0)));
    }

    // ─── RHC-23/29: apuramento mensal ─────────────────────────────────────────

    @Test
    void monthlySheet_splitsOvertimeByTier() { // RHC-23/29
        // Três dias úteis: um normal (8h), um com 2h extra a acabar de dia, um com 2h extra a
        // acabar dentro da janela nocturna. Somá-las perderia a distinção que decide o pagamento.
        withContract();
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of(
                entry(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(16, 0), 0),
                entry(LocalDate.of(YEAR, MONTH, 2), LocalTime.of(8, 0), LocalTime.of(18, 0), 0),
                entry(LocalDate.of(YEAR, MONTH, 3), LocalTime.of(12, 0), LocalTime.of(22, 0), 0)));

        TimeSheetDTO.TimeSheetLineDTO line = firstLine();

        assertEquals(3, line.workedDays());
        assertEquals(0, new BigDecimal("24.00").compareTo(line.normalHours()), "3 × 8h normais");
        assertEquals(0, new BigDecimal("2.00").compareTo(line.overtimeDayHours()));
        assertEquals(0, new BigDecimal("2.00").compareTo(line.overtimeNightHours()));
    }

    @Test
    void monthlySheet_restDayHoursAreAllExtraordinary() { // RHC-29
        // 2026-06-06 é sábado, dia de descanso neste horário: as 6 horas contam todas como
        // extraordinárias, nenhuma como normal.
        withContract();
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of(
                entry(LocalDate.of(YEAR, MONTH, 6), LocalTime.of(8, 0), LocalTime.of(14, 0), 0)));

        TimeSheetDTO.TimeSheetLineDTO line = firstLine();

        assertEquals(0, new BigDecimal("6.00").compareTo(line.restDayHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(line.normalHours()));
        assertEquals(0, BigDecimal.ZERO.compareTo(line.overtimeDayHours()));
    }

    @Test
    void monthlySheet_countsExpectedDaysAndMissingOnes() { // RHC-24 (a falta automática é o B2.2)
        withContract();
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of(
                entry(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(16, 0), 0)));

        TimeSheetDTO.TimeSheetLineDTO line = firstLine();

        // Junho de 2026 tem 22 dias úteis de segunda a sexta.
        assertEquals(22, line.expectedDays());
        assertEquals(1, line.workedDays());
        assertEquals(21, line.missingDays(), "dias previstos sem qualquer marcação");
    }

    @Test
    void monthlySheet_lateArrivalRespectsTolerance() {
        // Entrada prevista 08:00 com 10 minutos de tolerância: 08:09 não é atraso, 08:20 é.
        withContract();
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of(
                entry(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 9), LocalTime.of(17, 0), 0),
                entry(LocalDate.of(YEAR, MONTH, 2), LocalTime.of(8, 20), LocalTime.of(17, 0), 0)));

        assertEquals(1, firstLine().lateArrivals());
    }

    @Test
    void monthlySheet_ignoresEmployeeWithoutContractInPeriod() {
        // Mesma regra da folha salarial: sem contrato vigente não há expectativa nenhuma.
        when(employeeRepository.findByCompanyIdOrderByName(COMPANY)).thenReturn(List.of(employee()));
        when(contractService.findContractInPeriod(eq(5L), any(), any())).thenReturn(Optional.empty());
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of());

        assertEquals(0, service.getMonthlySheet(YEAR, MONTH).lines().size());
    }

    // ─── RHC-27/28: fecho e reabertura ────────────────────────────────────────

    @Test
    void closedPeriod_refusesNewEntries() { // RHC-28
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.of(closedSheet()));

        var ex = assertThrows(BusinessRuleException.class, () -> service.recordEntry(
                request(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(17, 0), 0)));
        assertEquals(true, ex.getMessage().contains("está fechada"));
        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void closingPeriod_isAudited() { // RHC-27
        withContract();
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.empty());
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of());

        service.closePeriod(YEAR, MONTH);

        verify(auditLogService).logCurrent(eq("TIMESHEET_CLOSE"), any());
    }

    @Test
    void reopeningWithoutReason_isBlocked() { // RHC-28
        var ex = assertThrows(BusinessRuleException.class, () -> service.reopenPeriod(YEAR, MONTH, "  "));
        assertEquals(true, ex.getMessage().contains("motivo"));
    }

    @Test
    void reopening_isAuditedWithTheReason() { // RHC-28
        withContract();
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.of(closedSheet()));
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of());

        service.reopenPeriod(YEAR, MONTH, "Marcação em falta do dia 12");

        verify(auditLogService).logCurrent(eq("TIMESHEET_REOPEN"), any());
    }

    // ─── RHC-24: a falta nasce do ponto, não é digitada ───────────────────────

    @Test
    void closing_generatesPendingAbsencesForDaysWithoutEntries() { // RHC-24
        withContract();
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.empty());
        // Só o dia 1 tem marcação; os outros 21 dias úteis de Junho ficam sem.
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of(
                entry(LocalDate.of(YEAR, MONTH, 1), LocalTime.of(8, 0), LocalTime.of(16, 0), 0)));
        when(timeEntryRepository.existsByCompanyIdAndEmployeeIdAndEntryDate(
                eq(COMPANY), eq(5L), eq(LocalDate.of(YEAR, MONTH, 1)))).thenReturn(true);
        when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

        service.closePeriod(YEAR, MONTH);

        ArgumentCaptor<Absence> saved = ArgumentCaptor.forClass(Absence.class);
        verify(absenceRepository, times(21)).save(saved.capture());
        Absence first = saved.getAllValues().get(0);
        // Nasce POR JUSTIFICAR e não INJUSTIFICADA: o desconto só olha para as injustificadas,
        // pelo que uma ausência ainda por explicar não tira dinheiro a ninguém antes de alguém decidir.
        assertEquals("PENDING_JUSTIFICATION", first.getAbsenceType());
        assertEquals(1, first.getTotalDays());
    }

    @Test
    void closing_doesNotDuplicateAbsencesThatAlreadyExist() { // RHC-24
        withContract();
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.empty());
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of());
        when(absenceRepository.existsByEmployeeIdAndStartDate(eq(5L), any())).thenReturn(true);

        service.closePeriod(YEAR, MONTH);

        verify(absenceRepository, never()).save(any(Absence.class));
    }

    @Test
    void closing_doesNotCreateAbsencesOnRestDays() { // RHC-24
        withContract();
        when(timeSheetRepository.findByCompanyIdAndYearAndMonth(COMPANY, YEAR, MONTH))
                .thenReturn(Optional.empty());
        when(timeEntryRepository.findByPeriod(eq(COMPANY), any(), any())).thenReturn(List.of());
        when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));

        service.closePeriod(YEAR, MONTH);

        ArgumentCaptor<Absence> saved = ArgumentCaptor.forClass(Absence.class);
        verify(absenceRepository, times(22)).save(saved.capture());
        // Junho de 2026 tem 30 dias e 22 úteis: nenhum sábado ou domingo pode ter gerado falta.
        assertEquals(0, saved.getAllValues().stream()
                .filter(a -> a.getStartDate().getDayOfWeek().getValue() >= 6).count());
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private void withContract() {
        when(employeeRepository.findByCompanyIdOrderByName(COMPANY)).thenReturn(List.of(employee()));
        when(contractService.findContractInPeriod(eq(5L), any(), any()))
                .thenReturn(Optional.of(new EmploymentContract()));
    }

    private TimeSheetDTO.TimeSheetLineDTO firstLine() {
        return service.getMonthlySheet(YEAR, MONTH).lines().get(0);
    }

    private CreateTimeEntryRequest request(LocalDate date, LocalTime in, LocalTime out, int breakMinutes) {
        return new CreateTimeEntryRequest(5L, date, in, out, breakMinutes, null, null);
    }

    private TimeEntry entry(LocalDate date, LocalTime in, LocalTime out, int breakMinutes) {
        TimeEntry entry = new TimeEntry();
        entry.setId(date.getDayOfMonth() + 0L);
        entry.setEmployee(employee());
        entry.setEntryDate(date);
        entry.setCheckIn(in);
        entry.setCheckOut(out);
        entry.setBreakMinutes(breakMinutes);
        entry.setRecordedBy("gestor");
        return entry;
    }

    private TimeSheet closedSheet() {
        TimeSheet sheet = new TimeSheet();
        sheet.setYear(YEAR);
        sheet.setMonth(MONTH);
        sheet.setStatus(TimeSheetStatus.FECHADA);
        sheet.setClosedBy("gestor");
        return sheet;
    }

    private static WorkSchedule schedule() {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setName("Padrão");
        return schedule;
    }

    private static Employee employee() {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-5");
        employee.setName("Maria");
        employee.setStatus("ACTIVE");
        employee.setBaseSalary(new BigDecimal("30000"));
        return employee;
    }
}

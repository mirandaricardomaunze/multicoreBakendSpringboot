package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.TimeEntry;
import mz.multicore.erp.modules.hr.model.TimeEntrySource;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponto e assiduidade. Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p><b>As horas extra do recibo têm de ter origem.</b> Hoje {@code CreatePayslipRequest.overtime} é
 * um número que quem processa a folha escreve à mão: ninguém sabe de onde veio, ninguém o pode
 * contestar, e o mesmo valor pode ser pago duas vezes sem que nada o note. Este serviço constrói a
 * origem — marcações datadas, com autor e proveniência, apuradas contra um horário.
 *
 * <p>Os totais <b>nunca são gravados</b>: apuram-se sempre das marcações. Gravá-los criaria uma
 * segunda verdade que se desactualiza à primeira correcção.
 */
@Service
public class TimeSheetService {

    /** Falta nascida do ponto e ainda por explicar. Não desconta — só as UNJUSTIFIED descontam. */
    public static final String PENDING_JUSTIFICATION = "PENDING_JUSTIFICATION";

    private final TimeEntryRepository timeEntryRepository;
    private final TimeSheetRepository timeSheetRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final EmploymentContractService contractService;
    private final AbsenceRepository absenceRepository;
    private final AuditLogService auditLogService;

    public TimeSheetService(TimeEntryRepository timeEntryRepository,
                            TimeSheetRepository timeSheetRepository,
                            WorkScheduleRepository workScheduleRepository,
                            EmployeeRepository employeeRepository,
                            CompanyRepository companyRepository,
                            EmploymentContractService contractService,
                            AbsenceRepository absenceRepository,
                            AuditLogService auditLogService) {
        this.timeEntryRepository = timeEntryRepository;
        this.timeSheetRepository = timeSheetRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.contractService = contractService;
        this.absenceRepository = absenceRepository;
        this.auditLogService = auditLogService;
    }

    // ─── Marcações ────────────────────────────────────────────────────────────

    @Transactional
    public TimeEntryDTO recordEntry(CreateTimeEntryRequest request) {
        ensureHrManager();
        Long companyId = currentCompanyId();
        Employee employee = findEmployee(request.employeeId());
        ensurePeriodOpen(request.entryDate().getYear(), request.entryDate().getMonthValue());

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
        ensureHrManager();
        Long companyId = currentCompanyId();
        TimeEntry entry = timeEntryRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Marcação não encontrada."));
        ensurePeriodOpen(entry.getEntryDate().getYear(), entry.getEntryDate().getMonthValue());

        String detail = String.format("Marcação de %s em %s (%s–%s) eliminada",
                entry.getEmployee().getName(), entry.getEntryDate(),
                entry.getCheckIn(), entry.getCheckOut());
        timeEntryRepository.delete(entry);
        auditLogService.logCurrent("TIME_ENTRY_DELETE", detail);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getEntries(int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        return timeEntryRepository.findByPeriod(currentCompanyId(), from, endOfMonth(from)).stream()
                .map(this::toDTO).toList();
    }

    /**
     * Uma marcação que não fecha não é uma marcação. A saída anterior à entrada é aceite como turno
     * que atravessa a meia-noite — mas a pausa não pode engolir o turno todo.
     */
    private void validateEntry(TimeEntry entry) {
        if (entry.getCheckIn().equals(entry.getCheckOut())) {
            throw new BusinessRuleException("A entrada e a saída não podem ser à mesma hora.");
        }
        if (entry.workedHours().signum() <= 0) {
            throw new BusinessRuleException(
                    "A pausa é igual ou superior ao tempo entre a entrada e a saída.");
        }
    }

    // ─── Folha de ponto do mês ────────────────────────────────────────────────

    /**
     * Apura o mês: por colaborador, dias previstos e trabalhados, horas normais e horas extra
     * <b>separadas por escalão</b>. Os escalões vêm separados porque a lei os trata de maneira
     * diferente — somá-los perderia justamente a informação que decide quanto se paga.
     */
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
            // Sem contrato vigente no mês não há expectativa nenhuma — a mesma regra da folha salarial.
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

    /**
     * A linha de um só colaborador. É o que a valorização das horas extra pergunta — apurar o mês
     * inteiro para ler uma linha seria trabalho a mais por recibo.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<TimeSheetDTO.TimeSheetLineDTO> findEmployeeLine(Long employeeId, int year, int month) {
        return getMonthlySheet(year, month).lines().stream()
                .filter(line -> line.employeeId().equals(employeeId))
                .findFirst();
    }

    private TimeSheetDTO.TimeSheetLineDTO computeLine(Employee employee, List<TimeEntry> entries,
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
                    // Dia previsto sem marcação: candidato a falta (a geração automática é o B2.2).
                    missingDays++;
                }
                continue;
            }

            workedDays++;
            BigDecimal worked = entry.workedHours();
            workedHours = workedHours.add(worked);

            if (restDay) {
                // Tudo o que se trabalha num dia de descanso é extraordinário, hora a hora.
                restDayHours = restDayHours.add(worked);
                continue;
            }

            BigDecimal normal = worked.min(dayExpected);
            normalHours = normalHours.add(normal);
            BigDecimal extra = worked.subtract(normal);
            if (extra.signum() > 0) {
                // O escalão da hora extra decide-se pela hora a que ela é prestada: as horas extra
                // são as últimas do turno, pelo que se olha para o fim dele.
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

    /**
     * Atraso é entrar depois da hora prevista mais a tolerância. A tolerância existe para o sistema
     * não transformar dois minutos de trânsito num incidente disciplinar.
     */
    private boolean isLate(TimeEntry entry, WorkSchedule schedule) {
        LocalTime limit = schedule.getExpectedStartTime().plusMinutes(schedule.getLateToleranceMinutes());
        return entry.getCheckIn().isAfter(limit);
    }

    // ─── Fecho e reabertura ───────────────────────────────────────────────────

    @Transactional
    public TimeSheetDTO closePeriod(int year, int month) {
        ensureHrManager();
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
        return getMonthlySheet(year, month);
    }

    /**
     * Um dia previsto sem marcação nasce falta <b>por justificar</b> — não digitada por ninguém.
     * Ver docs/RH_COMPLETO_SPEC.md §B2.
     *
     * <p>Nasce {@code PENDING_JUSTIFICATION} de propósito, e não {@code UNJUSTIFIED}: o desconto no
     * recibo só olha para as injustificadas, pelo que uma ausência ainda por explicar não tira
     * dinheiro a ninguém antes de alguém decidir. Presumir má-fé automaticamente seria a pior
     * forma de estrear o módulo.
     *
     * <p>Idempotente: fechar duas vezes não duplica faltas.
     */
    private int generateMissingDayAbsences(int year, int month) {
        Long companyId = currentCompanyId();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = endOfMonth(from);
        WorkSchedule schedule = defaultSchedule(companyId);

        int created = 0;
        for (TimeSheetDTO.TimeSheetLineDTO line : getMonthlySheet(year, month).lines()) {
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
                    continue; // Já há falta nesse dia — inclusive de um fecho anterior.
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

    /**
     * Reabrir é possível, mas exige motivo e fica auditado — senão o fecho não significa nada e a
     * folha salarial deixa de poder assentar nele.
     */
    @Transactional
    public TimeSheetDTO reopenPeriod(int year, int month, String reason) {
        ensureHrManager();
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
        return getMonthlySheet(year, month);
    }

    /** O período está fechado — a folha salarial pode assentar nele. Usado pelo B2.2. */
    @Transactional(readOnly = true)
    public boolean isPeriodClosed(int year, int month) {
        return timeSheetRepository.findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month)
                .map(TimeSheet::isClosed).orElse(false);
    }

    private void ensurePeriodOpen(int year, int month) {
        if (isPeriodClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "A folha de ponto de %d/%d está fechada. Reabra-a (com motivo) para a alterar.",
                    month, year));
        }
    }

    private TimeSheet findOrCreateSheet(int year, int month) {
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

    /**
     * Quantos <b>dias úteis</b> há entre duas datas, segundo o horário da empresa (§B8.1).
     *
     * <p>Vive aqui, e não no {@code HRService}, porque quem sabe o que é um dia de descanso é o
     * {@code WorkSchedule} — a mesma fonte que o ponto usa para classificar horas em dia de
     * descanso. Ter a resposta em dois sítios era exactamente o que fazia as férias contarem
     * dias de calendário enquanto o ponto contava dias úteis.
     */
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

    // ─── Apoio ────────────────────────────────────────────────────────────────

    /**
     * O horário da empresa. Sem nenhum configurado usa-se um por omissão em memória — o ponto tem
     * de conseguir apurar antes de alguém ter passado pela configuração, senão o módulo só arranca
     * depois de um passo que ninguém sabe que existe.
     */
    private WorkSchedule defaultSchedule(Long companyId) {
        return workScheduleRepository.findByCompanyIdOrderByName(companyId).stream().findFirst()
                .orElseGet(WorkSchedule::new);
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

    private static LocalDate endOfMonth(LocalDate monthStart) {
        return monthStart.withDayOfMonth(monthStart.lengthOfMonth());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
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

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem gerir o ponto.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TimeEntryDTO toDTO(TimeEntry entry) {
        return new TimeEntryDTO(
                entry.getId(), entry.getEmployee().getId(), entry.getEmployee().getName(),
                entry.getEntryDate(), entry.getCheckIn(), entry.getCheckOut(), entry.getBreakMinutes(),
                entry.workedHours(), entry.crossesMidnight(),
                entry.getSource().name(), entry.getSource().getLabel(),
                entry.getRecordedBy(), entry.getNotes());
    }
}

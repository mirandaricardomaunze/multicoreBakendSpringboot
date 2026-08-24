package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.SettlementStatus;
import mz.multicore.erp.modules.hr.model.Termination;
import mz.multicore.erp.modules.hr.model.TerminationReason;
import mz.multicore.erp.modules.hr.model.TerminationSettlementLine;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.TerminationRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cessação do vínculo e acerto final. Ver docs/RH_COMPLETO_SPEC.md §B3.
 *
 * <p><b>O que este serviço substitui:</b> uma String. {@code changeEmployeeStatus(id,"TERMINATED")}
 * e mais nada — sem acerto, sem proporcionais, sem documento. O sistema <b>já sabia</b> calcular o
 * 13.º proporcional e o saldo de férias; simplesmente nunca o fazia neste contexto, e por isso a
 * conta era feita à mão, em papel, ou não era feita.
 *
 * <p><b>O que não sabe calcular, diz.</b> O direito a férias por antiguidade e o aviso prévio vêm
 * da lei e são configuráveis (§6). Sem configuração, essas linhas não entram e o acerto sai com um
 * <b>aviso em PT-MZ</b> a dizer o que falta — um acerto que esconde o que não sabe calcular é muito
 * pior do que um acerto incompleto que o declara.
 */
@Service
public class TerminationService {

    /** Divisor mensal padrão para apurar o valor/dia a partir do salário base. */
    private static final BigDecimal MONTHLY_DIVISOR = BigDecimal.valueOf(30);
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    /** Valor histórico do direito anual, usado só quando a empresa ainda não configurou o seu. */
    private static final int FALLBACK_ANNUAL_VACATION_DAYS = 22;

    private final TerminationRepository terminationRepository;
    private final EmployeeRepository employeeRepository;
    private final VacationRepository vacationRepository;
    private final CompanyRepository companyRepository;
    private final EmploymentContractService contractService;
    private final SalaryHistoryService salaryHistoryService;
    private final PayrollDeductionService payrollDeductionService;
    private final HrPolicyService hrPolicyService;
    private final DocumentNumberService documentNumberService;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;

    public TerminationService(TerminationRepository terminationRepository,
                              EmployeeRepository employeeRepository,
                              VacationRepository vacationRepository,
                              CompanyRepository companyRepository,
                              EmploymentContractService contractService,
                              SalaryHistoryService salaryHistoryService,
                              PayrollDeductionService payrollDeductionService,
                              HrPolicyService hrPolicyService,
                              DocumentNumberService documentNumberService,
                              @Lazy FinanceService financeService,
                              AuditLogService auditLogService) {
        this.terminationRepository = terminationRepository;
        this.employeeRepository = employeeRepository;
        this.vacationRepository = vacationRepository;
        this.companyRepository = companyRepository;
        this.contractService = contractService;
        this.salaryHistoryService = salaryHistoryService;
        this.payrollDeductionService = payrollDeductionService;
        this.hrPolicyService = hrPolicyService;
        this.documentNumberService = documentNumberService;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
    }

    // ─── Apuramento ───────────────────────────────────────────────────────────

    /**
     * Mostra o acerto <b>antes</b> de o cometer. Existe porque cessar é irreversível e a conta é
     * conferida em papel contra o que o sistema apurou — obrigar a cessar primeiro para ver os
     * números seria pedir para descobrir o erro tarde de mais.
     */
    @Transactional(readOnly = true)
    public TerminationDTO preview(CreateTerminationRequest request) {
        ensureHrManager();
        Employee employee = findEmployee(request.employeeId());
        Settlement settlement = compute(employee, request);
        return previewDTO(employee, settlement, request);
    }

    /**
     * Cessa o vínculo: fecha o contrato, passa o colaborador a {@code TERMINATED} e grava o acerto.
     *
     * <p>O colaborador cessado deixa de poder ter recibos e pedidos de férias — não por uma guarda
     * nova, mas porque {@code findActiveEmployee} já exige {@code ACTIVE} nas duas portas. O que
     * faltava era a cessação chegar lá.
     */
    @Transactional
    public TerminationDTO terminate(CreateTerminationRequest request) {
        ensureHrManager();
        Long companyId = currentCompanyId();
        Employee employee = findEmployee(request.employeeId());
        if (terminationRepository.existsByCompanyIdAndEmployeeId(companyId, employee.getId())) {
            throw new BusinessRuleException(String.format(
                    "%s já tem uma cessação registada. Um colaborador cessa-se uma vez.",
                    employee.getName()));
        }
        if (request.terminationDate().isBefore(employee.getHireDate())) {
            throw new BusinessRuleException("A saída não pode ser anterior à admissão.");
        }

        Settlement settlement = compute(employee, request);

        Termination termination = new Termination();
        termination.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
        termination.setEmployee(employee);
        termination.setContract(settlement.contract);
        termination.setSettlementNumber(documentNumberService.next(DocumentSeries.TERMINATION));
        termination.setTerminationDate(request.terminationDate());
        termination.setReason(settlement.reason);
        termination.setNoticeServed(request.noticeServed());
        termination.setNotes(blankToNull(request.notes()));
        settlement.lines.forEach(termination::addLine);
        termination.recalculateTotals();

        Termination saved = terminationRepository.save(termination);

        // Cessa-se um contrato, não um estado. Quem não tem contrato registado (anterior ao B1)
        // cessa-se na mesma — e o acerto di-lo, em vez de recusar a saída de quem já saiu.
        if (settlement.contract != null) {
            contractService.terminateContract(settlement.contract.getId(), request.terminationDate(),
                    settlement.reason.getLabel());
        }
        employee.setStatus("TERMINATED");
        employee.setContractEndDate(request.terminationDate());
        employeeRepository.save(employee);

        auditLogService.logCurrent("TERMINATION_CREATE", String.format(
                "Cessação de %s em %s (%s): acerto %s, ganhos %s, descontos %s, líquido %s",
                employee.getName(), request.terminationDate(), settlement.reason.getLabel(),
                saved.getSettlementNumber(), saved.getTotalEarnings(), saved.getTotalDeductions(),
                saved.getNetAmount()));
        return toDTO(saved, settlement.warnings);
    }

    /**
     * Paga o acerto: saída de tesouraria pela mesma porta do recibo, auditada, uma só vez.
     *
     * <p><b>Um acerto negativo não se paga.</b> Quem sai a dever à empresa não recebe nada — e
     * mandar 0,00 para a tesouraria fingiria um pagamento que não houve.
     */
    @Transactional
    public TerminationDTO paySettlement(Long id) {
        ensureHrManager();
        Termination termination = terminationRepository.findByIdWithLines(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Acerto final não encontrado."));
        if (termination.getStatus() == SettlementStatus.PAGO) {
            throw new BusinessRuleException(String.format(
                    "O acerto %s já foi pago a %s.",
                    termination.getSettlementNumber(), termination.getPaymentDate()));
        }
        if (termination.getNetAmount().signum() <= 0) {
            throw new BusinessRuleException(String.format(
                    "O acerto %s não tem valor a pagar (líquido %s). %s continua a dever este valor "
                            + "à empresa — cobre-o fora da folha.",
                    termination.getSettlementNumber(), termination.getNetAmount(),
                    termination.getEmployee().getName()));
        }

        termination.setStatus(SettlementStatus.PAGO);
        termination.setPaymentDate(LocalDate.now());
        Termination saved = terminationRepository.save(termination);

        financeService.registerAutoPayout(saved.getNetAmount(), String.format(
                "Acerto final %s - %s", saved.getSettlementNumber(), saved.getEmployee().getName()));
        auditLogService.logCurrent("TERMINATION_PAID", String.format(
                "Acerto final %s pago a %s, líquido %s",
                saved.getSettlementNumber(), saved.getEmployee().getName(), saved.getNetAmount()));
        return toDTO(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<TerminationDTO> list() {
        return terminationRepository.findAllByCompany(currentCompanyId()).stream()
                .map(t -> toDTO(t, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public Termination loadForPrint(Long id) {
        return terminationRepository.findByIdWithLines(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Acerto final não encontrado."));
    }

    // ─── O cálculo ────────────────────────────────────────────────────────────

    /** O que compõe o acerto, mais o que não foi possível calcular. */
    private record Settlement(EmploymentContract contract, TerminationReason reason,
                              List<TerminationSettlementLine> lines, List<String> warnings) {}

    private Settlement compute(Employee employee, CreateTerminationRequest request) {
        TerminationReason reason = parseReason(request.reason());
        LocalDate exit = request.terminationDate();
        List<TerminationSettlementLine> lines = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        EmploymentContract contract = contractService
                .findContractInPeriod(employee.getId(), exit, exit).orElse(null);
        if (contract == null) {
            warnings.add("Este colaborador não tem contrato vigente à data da saída. A cessação é "
                    + "registada na mesma, mas nada fica ligado a um contrato.");
        }

        BigDecimal salary = salaryHistoryService.salaryOn(employee.getId(), exit)
                .orElseGet(() -> employee.getBaseSalary() == null ? BigDecimal.ZERO : employee.getBaseSalary());
        BigDecimal dailyRate = salary.divide(MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);

        addSalaryUntilExit(lines, salary, exit);
        addProportionalThirteenth(lines, salary, employee, exit);
        addUnusedVacation(lines, dailyRate, employee, exit, warnings);
        addCompensation(lines, request);
        addUnservedNotice(lines, dailyRate, reason, request, exit, warnings);
        addOutstandingDeductions(lines, employee);

        return new Settlement(contract, reason, lines, warnings);
    }

    /** Salário do mês até ao dia da saída — quem sai a 12 trabalhou 12 dias, não o mês inteiro. */
    private void addSalaryUntilExit(List<TerminationSettlementLine> lines, BigDecimal salary,
                                    LocalDate exit) {
        int daysWorked = exit.getDayOfMonth();
        int daysInMonth = exit.lengthOfMonth();
        BigDecimal amount = salary.multiply(BigDecimal.valueOf(daysWorked))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Salário de %02d/%d até %s (%d de %d dias)",
                exit.getMonthValue(), exit.getYear(), exit, daysWorked, daysInMonth), amount, true));
    }

    /** 13.º proporcional aos meses completos do ano de saída. */
    private void addProportionalThirteenth(List<TerminationSettlementLine> lines, BigDecimal salary,
                                           Employee employee, LocalDate exit) {
        LocalDate yearStart = LocalDate.of(exit.getYear(), 1, 1);
        LocalDate from = employee.getHireDate() != null && employee.getHireDate().isAfter(yearStart)
                ? employee.getHireDate() : yearStart;
        int months = Math.max(0, exit.getMonthValue() - from.getMonthValue() + 1);
        if (months <= 0) {
            return;
        }
        BigDecimal amount = salary.multiply(BigDecimal.valueOf(months))
                .divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP);
        lines.add(line(String.format("13.º mês proporcional (%d de 12 meses de %d)",
                months, exit.getYear()), amount, true));
    }

    /**
     * Férias vencidas e não gozadas. O direito anual vem da configuração da empresa (§6); sem ela
     * usa-se o valor histórico de {@value #FALLBACK_ANNUAL_VACATION_DAYS} dias — e <b>diz-se que é
     * um valor por omissão</b>, para ninguém confundir um número herdado com um número decidido.
     */
    private void addUnusedVacation(List<TerminationSettlementLine> lines, BigDecimal dailyRate,
                                   Employee employee, LocalDate exit, List<String> warnings) {
        int completedYears = employee.getHireDate() == null
                ? 0 : (int) ChronoUnit.YEARS.between(employee.getHireDate(), exit);
        Optional<Integer> configured = hrPolicyService.annualVacationDays(completedYears, exit);
        int entitlement = configured.orElse(FALLBACK_ANNUAL_VACATION_DAYS);
        if (configured.isEmpty()) {
            warnings.add(String.format(
                    "O direito anual de férias não está configurado para %d ano(s) de casa. O acerto "
                            + "usa o valor histórico de %d dias — confirme-o com o contabilista em "
                            + "RH › Valores Legais.", completedYears, FALLBACK_ANNUAL_VACATION_DAYS));
        }
        int taken = vacationRepository.sumReservedDays(employee.getId(), exit.getYear());
        int unused = entitlement - taken;
        if (unused <= 0) {
            return;
        }
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(unused)).setScale(2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Férias vencidas e não gozadas (%d de %d dias de %d)",
                unused, entitlement, exit.getYear()), amount, true));
    }

    private void addCompensation(List<TerminationSettlementLine> lines,
                                 CreateTerminationRequest request) {
        if (request.compensationAmount() == null || request.compensationAmount().signum() <= 0) {
            return;
        }
        lines.add(line("Compensação por cessação", request.compensationAmount(), true));
    }

    /**
     * Aviso prévio não cumprido — <b>a descontar</b>, e só quando era o trabalhador a devê-lo.
     * Descontá-lo a quem foi despedido seria cobrar-lhe a decisão da empresa.
     */
    private void addUnservedNotice(List<TerminationSettlementLine> lines, BigDecimal dailyRate,
                                   TerminationReason reason, CreateTerminationRequest request,
                                   LocalDate exit, List<String> warnings) {
        if (request.noticeServed() || !reason.isNoticeOwedByEmployee()) {
            return;
        }
        Optional<Integer> noticeDays = hrPolicyService.noticeDays(false, exit);
        if (noticeDays.isEmpty()) {
            warnings.add("O aviso prévio não foi cumprido, mas o número de dias não está configurado "
                    + "— nada foi descontado. Configure-o em RH › Valores Legais e refaça o acerto.");
            return;
        }
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(noticeDays.get()))
                .setScale(2, RoundingMode.HALF_UP);
        lines.add(line(String.format("Aviso prévio não cumprido (%d dias)", noticeDays.get()),
                amount, false));
    }

    /**
     * Adiantamentos e empréstimos por liquidar. Sem esta linha, quem sai a meio de um empréstimo
     * <b>levava o saldo consigo</b> — o dinheiro tinha saído da caixa e não voltava por porta nenhuma.
     */
    private void addOutstandingDeductions(List<TerminationSettlementLine> lines, Employee employee) {
        for (PayrollDeductionDTO deduction : payrollDeductionService.outstandingFor(employee.getId())) {
            lines.add(line(deduction.kindLabel() + " por liquidar — " + deduction.description(),
                    deduction.outstandingAmount(), false));
        }
    }

    // ─── Apoio ────────────────────────────────────────────────────────────────

    private static TerminationSettlementLine line(String description, BigDecimal amount, boolean earning) {
        TerminationSettlementLine line = new TerminationSettlementLine();
        line.setDescription(description);
        line.setAmount(amount);
        line.setEarning(earning);
        return line;
    }

    private TerminationReason parseReason(String raw) {
        try {
            return TerminationReason.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Motivo de cessação desconhecido: " + raw);
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa activa."));
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem cessar vínculos e pagar acertos finais.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** DTO do que ainda não foi gravado — sem id nem número, porque ainda não existe documento. */
    private TerminationDTO previewDTO(Employee employee, Settlement settlement,
                                      CreateTerminationRequest request) {
        BigDecimal earnings = settlement.lines.stream().filter(TerminationSettlementLine::isEarning)
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deductions = settlement.lines.stream().filter(l -> !l.isEarning())
                .map(TerminationSettlementLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TerminationDTO(null, null, employee.getId(), employee.getName(),
                settlement.contract == null ? null : settlement.contract.getId(),
                settlement.contract == null ? null : settlement.contract.getContractNumber(),
                request.terminationDate(), settlement.reason.name(), settlement.reason.getLabel(),
                request.noticeServed(), earnings, deductions, earnings.subtract(deductions),
                SettlementStatus.POR_PAGAR.name(), SettlementStatus.POR_PAGAR.getLabel(), null,
                blankToNull(request.notes()),
                settlement.lines.stream().map(TerminationService::lineDTO).toList(),
                settlement.warnings);
    }

    private TerminationDTO toDTO(Termination t, List<String> warnings) {
        return new TerminationDTO(t.getId(), t.getSettlementNumber(), t.getEmployee().getId(),
                t.getEmployee().getName(),
                t.getContract() == null ? null : t.getContract().getId(),
                t.getContract() == null ? null : t.getContract().getContractNumber(),
                t.getTerminationDate(), t.getReason().name(), t.getReason().getLabel(),
                t.isNoticeServed(), t.getTotalEarnings(), t.getTotalDeductions(), t.getNetAmount(),
                t.getStatus().name(), t.getStatus().getLabel(), t.getPaymentDate(), t.getNotes(),
                t.getLines().stream().map(TerminationService::lineDTO).toList(), warnings);
    }

    private static TerminationDTO.TerminationLineDTO lineDTO(TerminationSettlementLine line) {
        return new TerminationDTO.TerminationLineDTO(
                line.getDescription(), line.getAmount(), line.isEarning());
    }
}

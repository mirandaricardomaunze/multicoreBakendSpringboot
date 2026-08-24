package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.events.PayslipPaidEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.PayrollRunDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.model.Absence;
import mz.multicore.erp.modules.hr.model.AbsencePayRule;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.ExpenseClaim;
import mz.multicore.erp.modules.hr.model.ExpenseStatus;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.model.Vacation;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.ExpenseClaimRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HRService {

    /**
     * Direito anual de férias usado <b>só quando a empresa ainda não configurou o seu</b> (§B8.2).
     * Era uma constante compilada e a única resposta possível; passou a ser o último recurso, e a
     * mensagem ao utilizador diz que é um valor por omissão — a lei laboral moçambicana faz o
     * direito crescer com a antiguidade, pelo que um número fixo está errado para alguém.
     */
    private static final int FALLBACK_ANNUAL_VACATION_DAYS = 22;

    private final EmployeeRepository employeeRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final PayslipRepository payslipRepository;
    private final AbsenceRepository absenceRepository;
    private final VacationRepository vacationRepository;
    private final CompanyRepository companyRepository;
    private final PayrollTaxService payrollTaxService;
    private final ApprovalService approvalService;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final FinanceService financeService;
    private final AppUserService appUserService;
    private final EmploymentContractService contractService;
    private final TimeSheetService timeSheetService;
    private final OvertimeValuationService overtimeValuationService;
    private final SalaryHistoryService salaryHistoryService;
    private final PayrollLiabilityService payrollLiabilityService;
    private final PayrollDeductionService payrollDeductionService;
    private final HrPolicyService hrPolicyService;
    private final PayrollPeriodService payrollPeriodService;
    private final ApplicationEventPublisher eventPublisher;

    public HRService(
            EmployeeRepository employeeRepository,
            ExpenseClaimRepository expenseClaimRepository,
            PayslipRepository payslipRepository,
            AbsenceRepository absenceRepository,
            VacationRepository vacationRepository,
            CompanyRepository companyRepository,
            PayrollTaxService payrollTaxService,
            @Lazy ApprovalService approvalService, // Lazy injection to break potential cycles
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            @Lazy FinanceService financeService,
            AppUserService appUserService,
            EmploymentContractService contractService,
            @Lazy TimeSheetService timeSheetService,
            @Lazy OvertimeValuationService overtimeValuationService,
            SalaryHistoryService salaryHistoryService,
            @Lazy PayrollLiabilityService payrollLiabilityService,
            @Lazy PayrollDeductionService payrollDeductionService,
            HrPolicyService hrPolicyService,
            PayrollPeriodService payrollPeriodService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.employeeRepository = employeeRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.payslipRepository = payslipRepository;
        this.absenceRepository = absenceRepository;
        this.vacationRepository = vacationRepository;
        this.companyRepository = companyRepository;
        this.payrollTaxService = payrollTaxService;
        this.approvalService = approvalService;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.financeService = financeService;
        this.appUserService = appUserService;
        this.contractService = contractService;
        this.timeSheetService = timeSheetService;
        this.overtimeValuationService = overtimeValuationService;
        this.salaryHistoryService = salaryHistoryService;
        this.payrollLiabilityService = payrollLiabilityService;
        this.payrollDeductionService = payrollDeductionService;
        this.hrPolicyService = hrPolicyService;
        this.payrollPeriodService = payrollPeriodService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        Employee employee = findEmployee(request.employeeId());
        ensureCanActFor(employee);

        ExpenseClaim claim = new ExpenseClaim();
        claim.setEmployee(employee);
        claim.setAmount(request.amount());
        claim.setCategory(request.category());
        claim.setDescription(request.description());
        claim.setStatus(ExpenseStatus.PENDING_APPROVAL);

        claim = expenseClaimRepository.save(claim);

        // Submit to Approvals Engine
        String approvalDesc = String.format("Despesa de %s (%s) - %s",
                employee.getName(), claim.getCategory(), claim.getDescription());
        approvalService.submitRequest("EXPENSE", claim.getId(), claim.getAmount(), approvalDesc);

        // Auditoria nomeia quem submeteu E por quem, porque ainda são coisas diferentes: sem ligação
        // Employee↔User, um utilizador consegue submeter em nome de um colega. Enquanto isso não fecha
        // (B7.2 da RH_COMPLETO_SPEC), o rasto deixa a substituição visível a quem audita.
        auditLogService.logCurrent("EXPENSE_SUBMIT", String.format(
                "Despesa de %s submetida por %s: %s %s (%s)",
                employee.getName(), CurrentUserContext.getUsername(),
                claim.getAmount(), claim.getCategory(), claim.getDescription()));

        return toDTO(claim);
    }

    @Transactional(readOnly = true)
    public List<ExpenseClaimDTO> getAllExpenses() {
        return expenseClaimRepository.findAllWithEmployeeByCompanyId(currentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findByCompanyIdOrderByName(currentCompanyId())
                .stream()
                .map(this::employeeToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeDTO createEmployee(UpsertEmployeeRequest request) {
        ensureHrManager();
        Long companyId = currentCompanyId();
        validateEmployeeUniqueness(companyId, null, request);

        Employee employee = new Employee();
        employee.setCompany(currentCompany());
        employee.setStatus("ACTIVE");
        applyEmployee(employee, request);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_CREATE", String.format(
                "Colaborador %s (nº %s) criado", saved.getName(), saved.getEmployeeNumber()));
        return employeeToDTO(saved);
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest request) {
        ensureHrManager();
        Long companyId = currentCompanyId();
        Employee employee = findEmployee(id);
        validateEmployeeUniqueness(companyId, id, request);
        applyEmployee(employee, request);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_UPDATE", String.format(
                "Colaborador %s (nº %s) actualizado", saved.getName(), saved.getEmployeeNumber()));
        return employeeToDTO(saved);
    }

    @Transactional
    public EmployeeDTO changeEmployeeStatus(Long id, String status) {
        ensureHrManager();
        Employee employee = findEmployee(id);
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "SUSPENDED", "TERMINATED").contains(normalized)) {
            throw new BusinessRuleException("Estado laboral inválido.");
        }
        employee.setStatus(normalized);
        Employee saved = employeeRepository.save(employee);
        auditLogService.logCurrent("EMPLOYEE_STATUS", String.format(
                "Estado laboral de %s (nº %s) alterado para %s",
                saved.getName(), saved.getEmployeeNumber(), normalized));
        return employeeToDTO(saved);
    }

    private ExpenseClaimDTO toDTO(ExpenseClaim claim) {
        return new ExpenseClaimDTO(
                claim.getId(),
                claim.getEmployee().getId(),
                claim.getEmployee().getName(),
                claim.getAmount(),
                claim.getCategory(),
                claim.getDescription(),
                claim.getStatus(),
                claim.getRejectionReason(),
                claim.getCreatedAt() != null ? claim.getCreatedAt() : LocalDateTime.now()
        );
    }

    // ─── Payslips ──────────────────────────────────────────────────────────────

    @Transactional
    public PayslipDTO createPayslip(CreatePayslipRequest request) {
        ensureHrManager();
        // Um mês já pago, entregue ao Estado e contabilizado não aceita recibos novos (§B8.6).
        payrollPeriodService.ensureOpen(request.year(), request.month());
        Employee employee = findActiveEmployee(request.employeeId());

        if (payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), request.year(), request.month()).isPresent()) {
            throw new BusinessRuleException(
                    "Já existe um recibo para este colaborador em " + request.month() + "/" + request.year() + ".");
        }

        Payslip p = new Payslip();
        p.setEmployee(employee);
        p.setCompany(employee.getCompany()); // recibo pertence à empresa do colaborador (numeração por empresa)
        p.setYear(request.year());
        p.setMonth(request.month());
        p.setBaseSalary(salaryForPeriod(employee, request.year(), request.month()));
        p.setAllowances(orZero(request.allowances()));
        p.setOvertime(resolveOvertime(employee, request, p));
        var tax = payrollTaxService.calculate(employee, p.getAllowances(), p.getOvertime(), request.year(), request.month());
        p.setTaxableIncome(tax.taxableIncome());
        p.setIrpsDeduction(tax.irps());
        p.setInssDeduction(tax.employeeInss());
        p.setEmployerInss(tax.employerInss());
        p.setIrpsRate(tax.irpsRate());
        p.setEmployeeInssRate(tax.employeeInssRate());
        p.setEmployerInssRate(tax.employerInssRate());
        p.setTaxConfigName(tax.configName());
        p.setTaxLegalBasis(tax.legalBasis());
        p.setOtherDeductions(orZero(request.otherDeductions()));
        p.setAbsenceDeduction(calculateAbsenceDeduction(employee, request.year(), request.month()));
        p.setNetPay(calculateNet(p));
        p.setStatus("DRAFT");
        p.setNotes(request.notes());
        p.setPayslipNumber(documentNumberService.next(DocumentSeries.PAYSLIP));

        Payslip saved = payslipRepository.save(p);
        saved = applyCommittedDeductions(saved);
        auditLogService.logCurrent("PAYSLIP_ISSUE", String.format(
                "Recibo %s emitido para %s (%d/%d), líquido %s",
                saved.getPayslipNumber(), employee.getName(), request.month(), request.year(), saved.getNetPay()));
        return payslipToDTO(saved);
    }

    /**
     * Aplica ao recibo os adiantamentos, empréstimos e descontos recorrentes vigentes (§B6). Corre
     * <b>depois</b> de o recibo estar gravado, porque cada linha aponta para ele.
     *
     * <p>A margem disponível é o que sobra depois do IRPS, do INSS, das faltas e do desconto manual:
     * <b>nunca se desconta mais do que o colaborador tem a receber</b>. O que não couber continua em
     * dívida — não é erro, é a única coisa honesta a fazer com um líquido que não chega.
     */
    private Payslip applyCommittedDeductions(Payslip payslip) {
        BigDecimal gross = payslip.getBaseSalary().add(payslip.getAllowances()).add(payslip.getOvertime());
        BigDecimal room = gross
                .subtract(payslip.getIrpsDeduction())
                .subtract(payslip.getInssDeduction())
                .subtract(payslip.getAbsenceDeduction())
                .subtract(payslip.getOtherDeductions());
        BigDecimal committed = payrollDeductionService.applyTo(payslip, room.max(BigDecimal.ZERO));
        if (committed.signum() <= 0) {
            return payslip;
        }
        payslip.setOtherDeductions(payslip.getOtherDeductions().add(committed));
        payslip.setNetPay(calculateNet(payslip));
        return payslipRepository.save(payslip);
    }

    /**
     * <b>O recibo de Março usa o salário de Março.</b> Reprocessar um mês antigo depois de um
     * aumento pagava, até agora, ao valor de hoje — e nada parecia errado, porque o número era
     * perfeitamente normal. A referência é o <b>último dia do período</b>: um aumento com efeito a
     * meio do mês vale para esse mês inteiro (o proporcional ao dia é assunto do B5).
     *
     * <p>Sem histórico nenhum — colaboradores anteriores a este bloco — vale o valor da ficha, que
     * é o comportamento de sempre.
     */
    private BigDecimal salaryForPeriod(Employee employee, int year, int month) {
        LocalDate periodEnd = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        return salaryHistoryService.salaryOn(employee.getId(), periodEnd)
                .orElseGet(() -> employee.getBaseSalary() != null ? employee.getBaseSalary() : BigDecimal.ZERO);
    }

    /**
     * A folha salarial não corre com o ponto do mês por fechar — senão paga sobre horas que ainda
     * podem mudar. <b>Só bloqueia quando há ponto marcado nesse mês:</b> uma empresa que não usa o
     * módulo não pode ficar sem conseguir processar salários por causa de um fecho que não lhe diz
     * respeito.
     */
    private void ensureTimeSheetClosed(int year, int month) {
        if (timeSheetService.getEntries(year, month).isEmpty()) {
            return;
        }
        if (!timeSheetService.isPeriodClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "A folha de ponto de %d/%d ainda está aberta. Feche-a antes de processar a folha "
                            + "salarial, para os salários não assentarem em horas que ainda podem mudar.",
                    month, year));
        }
    }

    /**
     * <b>As horas extra do recibo têm de ter origem.</b> Com a folha de ponto do mês <b>fechada</b>,
     * o valor vem apurado das marcações — não do que alguém escreve na caixa. O campo manual
     * sobrevive só como <b>excepção declarada</b>: exige justificação e fica auditado, exactamente
     * como o {@code taxRate} manual do {@code CreateInvoiceLineRequest}. A porta que era a regra
     * passa a ser a excepção.
     *
     * <p>Sem ponto fechado, o comportamento é o de sempre (valor manual) — senão emitir um recibo
     * avulso passaria a exigir um módulo inteiro que a loja pode ainda não usar.
     */
    private BigDecimal resolveOvertime(Employee employee, CreatePayslipRequest request, Payslip payslip) {
        BigDecimal manual = orZero(request.overtime());
        if (!timeSheetService.isPeriodClosed(request.year(), request.month())) {
            return manual;
        }
        var valuation = overtimeValuationService.valueFor(
                employee.getId(), request.year(), request.month(), payslip.getBaseSalary());
        if (valuation.isEmpty()) {
            return manual;
        }
        BigDecimal fromTimeSheet = valuation.get().amount();

        if (manual.signum() > 0 && manual.compareTo(fromTimeSheet) != 0) {
            if (request.overtimeOverrideReason() == null || request.overtimeOverrideReason().isBlank()) {
                throw new BusinessRuleException(String.format(
                        "A folha de ponto de %d/%d está fechada e apura %s de horas extra. "
                                + "Para usar outro valor indique a justificação da excepção.",
                        request.month(), request.year(), fromTimeSheet));
            }
            auditLogService.logCurrent("PAYSLIP_OVERTIME_OVERRIDE", String.format(
                    "Recibo de %s (%d/%d): horas extra manuais %s em vez das %s apuradas no ponto. Justificação: %s",
                    employee.getName(), request.month(), request.year(), manual, fromTimeSheet,
                    request.overtimeOverrideReason().trim()));
            return manual;
        }
        return fromTimeSheet;
    }

    /**
     * <b>O contrato manda na folha.</b> Antes desta versão o filtro era só {@code status == ACTIVE}
     * na ficha, pelo que um colaborador cujo contrato terminou a 31 de Julho recebia recibo em
     * Agosto — em silêncio, com saída de tesouraria. Agora quem não tem contrato vigente no mês é
     * saltado, e o resultado <b>diz quem e porquê</b>: "12 recibos gerados" que esconde um 13º
     * colaborador saltado só se descobre quando alguém reclama o ordenado.
     * Ver docs/RH_COMPLETO_SPEC.md §B1.
     */
    @Transactional
    public PayrollRunDTO processMonthlyPayroll(int year, int month) {
        ensureHrManager();
        ensureTimeSheetClosed(year, month);
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<PayslipDTO> generated = new ArrayList<>();
        List<PayrollRunDTO.PayrollSkipDTO> skipped = new ArrayList<>();

        for (Employee employee : employeeRepository.findByCompanyIdOrderByName(currentCompanyId())) {
            if (!"ACTIVE".equals(employee.getStatus())) {
                continue; // Inactivo é decisão já tomada e visível na ficha — não é surpresa nenhuma.
            }
            if (payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), year, month).isPresent()) {
                continue; // Já tem recibo deste mês: reprocessar não é saltar.
            }
            if (contractService.findContractInPeriod(employee.getId(), monthStart, monthEnd).isEmpty()) {
                skipped.add(new PayrollRunDTO.PayrollSkipDTO(employee.getId(), employee.getName(),
                        "Sem contrato vigente em " + month + "/" + year));
                continue;
            }
            generated.add(createPayslip(new CreatePayslipRequest(
                    employee.getId(), year, month, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Processamento mensal automático", null)));
        }

        auditLogService.logCurrent("PAYROLL_PROCESS", String.format(
                "Folha mensal %d/%d processada: %d recibo(s) gerado(s), %d colaborador(es) sem contrato vigente",
                month, year, generated.size(), skipped.size()));
        return new PayrollRunDTO(generated, skipped);
    }

    /**
     * Segunda vista antes de pagar (§B8.4). A {@code HR_PAYROLL_SPEC §3} prometia
     * {@code DRAFT → APPROVED → PAID} e o recibo só conhecia {@code DRAFT}, {@code PAID} e
     * {@code CANCELLED}: <b>quem processava a folha pagava-a sozinho</b>, sem que ninguém tivesse de
     * olhar para os números primeiro. É o mesmo raciocínio da aprovação da encomenda A4.
     */
    @Transactional
    public PayslipDTO approvePayslip(Long id) {
        ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BusinessRuleException("Apenas recibos em rascunho podem ser aprovados.");
        }
        p.setStatus("APPROVED");
        Payslip saved = payslipRepository.save(p);
        auditLogService.logCurrent("PAYSLIP_APPROVE", String.format(
                "Recibo %s de %s aprovado por %s, líquido %s",
                saved.getPayslipNumber(), saved.getEmployee().getName(),
                CurrentUserContext.getUsername(), saved.getNetPay()));
        return payslipToDTO(saved);
    }

    @Transactional
    public PayslipDTO markPayslipPaid(Long id) {
        ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"APPROVED".equals(p.getStatus())) {
            throw new BusinessRuleException(
                    "Só se paga um recibo aprovado. Aprove-o primeiro — é a segunda vista sobre os "
                            + "números antes de o dinheiro sair.");
        }
        p.setStatus("PAID");
        p.setPaymentDate(LocalDate.now());
        Payslip saved = payslipRepository.save(p);

        // Saída de tesouraria pelo líquido pago (espelho do reembolso de despesa).
        financeService.registerAutoPayout(saved.getNetPay(), String.format(
                "Pagamento Salário %s - %s", saved.getPayslipNumber(), saved.getEmployee().getName()));

        // Só o líquido saía daqui. O IRPS retido e o INSS das duas partes ficavam na conta da
        // empresa indistinguíveis de dinheiro próprio — sem obrigação registada, sem prazo, sem
        // aviso. Passam a existir como dívida ao Estado no mesmo acto em que são retidos (§B5).
        payrollLiabilityService.accrueForPeriod(saved.getYear(), saved.getMonth());

        // O lançamento contabilístico vai por evento, para o RH não passar a conhecer a
        // contabilidade — mesmo desenho do SaleRegisteredEvent (RHC-54).
        eventPublisher.publishEvent(new PayslipPaidEvent(
                currentCompanyId(), saved.getId(), saved.getPayslipNumber(),
                saved.getEmployee().getName(), saved.getPaymentDate(),
                saved.getBaseSalary().add(saved.getAllowances()).add(saved.getOvertime()),
                saved.getAbsenceDeduction(), saved.getIrpsDeduction(), saved.getInssDeduction(),
                saved.getEmployerInss(), saved.getOtherDeductions(), saved.getNetPay()));

        auditLogService.logCurrent("PAYSLIP_PAID", String.format(
                "Recibo %s pago a %s, líquido %s",
                saved.getPayslipNumber(), saved.getEmployee().getName(), saved.getNetPay()));
        return payslipToDTO(saved);
    }

    @Transactional
    public PayslipDTO cancelPayslip(Long id) {
        ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if ("PAID".equals(p.getStatus())) {
            throw new BusinessRuleException("Não é possível cancelar um recibo já pago.");
        }
        p.setStatus("CANCELLED");
        Payslip saved = payslipRepository.save(p);
        // Um recibo anulado não descontou nada: as prestações que levou voltam a estar em dívida.
        // Sem isto, o empréstimo ficava pago com dinheiro que ninguém chegou a descontar (§B6).
        payrollDeductionService.releaseFromPayslip(saved.getId());
        auditLogService.logCurrent("PAYSLIP_CANCEL", String.format(
                "Recibo %s cancelado (%s)", saved.getPayslipNumber(), saved.getEmployee().getName()));
        return payslipToDTO(saved);
    }

    /**
     * Gestores vêem a folha toda; quem não é gestor vê **só os seus recibos**. O salário de um colega
     * é das poucas coisas que nunca deve aparecer por omissão, e até à V48 aparecia a toda a gente.
     */
    @Transactional(readOnly = true)
    public List<PayslipDTO> getAllPayslips() {
        List<Payslip> payslips = payslipRepository.findAllWithEmployeeByCompanyId(currentCompanyId());
        if (!isHrManager()) {
            Long selfId = findSelfEmployee().map(Employee::getId).orElse(null);
            payslips = payslips.stream()
                    .filter(p -> selfId != null && selfId.equals(p.getEmployee().getId()))
                    .toList();
        }
        return payslips.stream().map(this::payslipToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Payslip loadPayslipForPrint(Long id) {
        Payslip payslip = payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        // Mesma regra da listagem: filtrar a lista e deixar imprimir por id era meia porta.
        ensureCanActFor(payslip.getEmployee());
        return payslip;
    }

    private BigDecimal calculateNet(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
        BigDecimal deductions = p.getIrpsDeduction().add(p.getInssDeduction())
                .add(p.getOtherDeductions()).add(p.getAbsenceDeduction());
        return gross.subtract(deductions);
    }

    /**
     * Desconto por faltas injustificadas (não remuneradas) que se sobrepõem ao mês do recibo.
     * Valor/dia = salário base / 30 (divisor mensal padrão MZ); só conta os dias dentro do mês.
     */
    private BigDecimal calculateAbsenceDeduction(Employee employee, int year, int month) {
        BigDecimal baseSalary = employee.getBaseSalary();
        if (baseSalary == null || baseSalary.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        int unpaidDays = absenceRepository
                .findUnpaidOverlapping(employee.getId(), periodStart, periodEnd,
                        AbsencePayRule.unpaidTypes()).stream()
                .mapToInt(a -> {
                    LocalDate from = a.getStartDate().isBefore(periodStart) ? periodStart : a.getStartDate();
                    LocalDate to = a.getEndDate().isAfter(periodEnd) ? periodEnd : a.getEndDate();
                    return (int) (ChronoUnit.DAYS.between(from, to) + 1);
                })
                .sum();
        if (unpaidDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal dailyRate = baseSalary.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private PayslipDTO payslipToDTO(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
        BigDecimal totalDeductions = p.getIrpsDeduction().add(p.getInssDeduction())
                .add(p.getOtherDeductions()).add(p.getAbsenceDeduction());
        return new PayslipDTO(
                p.getId(),
                p.getPayslipNumber(),
                p.getEmployee().getId(),
                p.getEmployee().getName(),
                p.getEmployee().getDepartment(),
                p.getYear(),
                p.getMonth(),
                p.getBaseSalary(),
                p.getAllowances(),
                p.getOvertime(),
                p.getIrpsDeduction(),
                p.getInssDeduction(),
                p.getEmployerInss(),
                p.getTaxableIncome(),
                p.getOtherDeductions(),
                p.getAbsenceDeduction(),
                gross,
                totalDeductions,
                p.getNetPay(),
                p.getStatus(),
                p.getPaymentDate(),
                p.getNotes()
        );
    }

    // ─── Absences ──────────────────────────────────────────────────────────────

    @Transactional
    public AbsenceDTO recordAbsence(CreateAbsenceRequest request) {
        // Lançar falta é acto de gestão sobre outra pessoa: nunca foi self-service, e sem guarda
        // qualquer utilizador da empresa marcava faltas a um colega.
        ensureHrManager();
        Employee employee = findActiveEmployee(request.employeeId());
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

    /**
     * Justifica uma falta nascida do ponto: muda-lhe o tipo, com motivo e indicação de documento.
     * Ver docs/RH_COMPLETO_SPEC.md §B2 (RHC-25).
     *
     * <p>É aqui que se decide se a ausência desconta ou não — uma falta gerada automaticamente
     * nasce por justificar justamente para que essa decisão seja de alguém, e fique com nome.
     */
    @Transactional
    public AbsenceDTO justifyAbsence(Long id, String absenceType, String reason, boolean hasDocument) {
        ensureHrManager();
        if (absenceType == null || absenceType.isBlank()) {
            throw new BusinessRuleException("Indique o tipo de falta.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Justificar uma falta exige um motivo.");
        }
        Absence absence = absenceRepository.findByIdAndEmployeeCompanyId(id, currentCompanyId())
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

    @Transactional
    public void deleteAbsence(Long id) {
        // Eliminar uma falta apaga o desconto que ela provoca no recibo. Sem guarda nem rasto, era a
        // porta mais silenciosa do RH: a falta desaparecia e o líquido subia sem ninguém saber porquê.
        ensureHrManager();
        Long companyId = currentCompanyId();
        Absence absence = absenceRepository.findByIdAndEmployeeCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Falta não encontrada."));
        String detail = String.format(
                "Falta %s de %s (%s a %s, %d dia(s)) eliminada",
                absence.getAbsenceType(), absence.getEmployee().getName(),
                absence.getStartDate(), absence.getEndDate(), absence.getTotalDays());

        absenceRepository.deleteByIdAndEmployeeCompanyId(id, companyId);
        auditLogService.logCurrent("ABSENCE_DELETE", detail);
    }

    @Transactional(readOnly = true)
    public List<AbsenceDTO> getAllAbsences() {
        return absenceRepository.findAllWithEmployeeByCompanyId(currentCompanyId()).stream()
                .map(this::absenceToDTO).collect(Collectors.toList());
    }

    private AbsenceDTO absenceToDTO(Absence a) {
        return new AbsenceDTO(
                a.getId(),
                a.getEmployee().getId(),
                a.getEmployee().getName(),
                a.getAbsenceType(),
                a.getStartDate(),
                a.getEndDate(),
                a.getTotalDays(),
                a.getReason(),
                a.isHasSupportingDocument()
        );
    }

    // ─── Vacations ─────────────────────────────────────────────────────────────

    @Transactional
    public VacationDTO submitVacation(CreateVacationRequest request) {
        // O colaborador a quem as férias pertencem vem do corpo do pedido: sem a regra abaixo, qualquer
        // utilizador pedia férias em nome de outro.
        Employee employee = findActiveEmployee(request.employeeId());
        ensureCanActFor(employee);
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        // Dias ÚTEIS, como a spec sempre prometeu. Contar dias de calendário fazia quem pedisse 22
        // dias seguidos gastar o ano inteiro, e quem partisse as férias em bocados sair a ganhar —
        // duas contas diferentes para o mesmo direito. Ver §B8.1.
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

    private int annualVacationDays(Employee employee, LocalDate referenceDate) {
        LocalDate hireDate = employee.getHireDate();
        int completedYears = hireDate == null || referenceDate.isBefore(hireDate)
                ? 0
                : (int) ChronoUnit.YEARS.between(hireDate, referenceDate);
        return hrPolicyService.annualVacationDays(completedYears, referenceDate)
                .orElse(FALLBACK_ANNUAL_VACATION_DAYS);
    }

    @Transactional
    public VacationDTO decideVacation(Long id, boolean approve, String rejectionReason) {
        ensureHrManager();
        Vacation v = vacationRepository.findByIdAndEmployeeCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"PENDING".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas pedidos pendentes podem ser decididos.");
        }
        if (!approve && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new BusinessRuleException("Indique o motivo da rejeição das férias.");
        }
        // O decisor vem sempre do contexto autenticado, nunca de input do cliente.
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

    @Transactional(readOnly = true)
    public List<VacationDTO> getAllVacations() {
        return vacationRepository.findAllWithEmployeeByCompanyId(currentCompanyId()).stream()
                .map(this::vacationToDTO).collect(Collectors.toList());
    }

    private VacationDTO vacationToDTO(Vacation v) {
        return new VacationDTO(
                v.getId(),
                v.getEmployee().getId(),
                v.getEmployee().getName(),
                v.getStartDate(),
                v.getEndDate(),
                v.getTotalDays(),
                v.getYearReference(),
                v.getStatus(),
                v.getNotes(),
                v.getDecisionBy(),
                v.getDecisionAt(),
                v.getRejectionReason()
        );
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa ativa."));
    }

    private Employee findActiveEmployee(Long id) {
        Employee employee = findEmployee(id);
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessRuleException("O colaborador não está ativo.");
        }
        return employee;
    }

    private Long currentCompanyId() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa ativa.");
        }
        return companyId;
    }

    private Company currentCompany() {
        return companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa ativa não encontrada."));
    }

    private void ensureHrManager() {
        if (!isHrManager()) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem executar esta operação de RH.");
        }
    }

    private boolean isHrManager() {
        String role = CurrentUserContext.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    /** O colaborador do utilizador autenticado, quando a conta está ligada a um (V48). */
    private Optional<Employee> findSelfEmployee() {
        String username = CurrentUserContext.getUsername();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return employeeRepository.findByCompanyIdAndAppUserUsername(currentCompanyId(), username);
    }

    /**
     * Um gestor age por qualquer colaborador; toda a gente age **por si própria** e por mais ninguém.
     * É esta regra que devolve o self-service sem reabrir o furo: até à V48, "o próprio" não era
     * identificável, pelo que agir por outro era indistinguível de agir por si.
     */
    private void ensureCanActFor(Employee employee) {
        if (isHrManager()) {
            return;
        }
        Employee self = findSelfEmployee().orElseThrow(() -> new BusinessRuleException(
                "A sua conta não está associada a nenhum colaborador. Peça ao RH para fazer a associação."));
        if (!self.getId().equals(employee.getId())) {
            throw new BusinessRuleException(
                    "Só pode submeter pedidos em seu próprio nome. Para o fazer por outro colaborador é "
                            + "preciso perfil de gestor ou administrador.");
        }
    }

    private void validateEmployeeUniqueness(Long companyId, Long employeeId, UpsertEmployeeRequest request) {
        boolean numberExists = employeeId == null
                ? employeeRepository.existsByCompanyIdAndEmployeeNumberIgnoreCase(companyId, request.employeeNumber())
                : employeeRepository.existsByCompanyIdAndEmployeeNumberIgnoreCaseAndIdNot(companyId, request.employeeNumber(), employeeId);
        if (numberExists) {
            throw new BusinessRuleException("Já existe um colaborador com este número interno.");
        }
        boolean emailExists = employeeId == null
                ? employeeRepository.existsByCompanyIdAndEmailIgnoreCase(companyId, request.email())
                : employeeRepository.existsByCompanyIdAndEmailIgnoreCaseAndIdNot(companyId, request.email(), employeeId);
        if (emailExists) {
            throw new BusinessRuleException("Já existe um colaborador com este email.");
        }
        if (request.contractEndDate() != null && request.contractEndDate().isBefore(request.hireDate())) {
            throw new BusinessRuleException("O fim do contrato não pode ser anterior à admissão.");
        }
    }

    private void applyEmployee(Employee employee, UpsertEmployeeRequest request) {
        employee.setEmployeeNumber(request.employeeNumber().trim());
        employee.setName(request.name().trim());
        employee.setEmail(request.email().trim().toLowerCase());
        employee.setPhone(blankToNull(request.phone()));
        employee.setPhoto(request.photo());
        employee.setTaxId(blankToNull(request.taxId()));
        employee.setInssNumber(blankToNull(request.inssNumber()));
        employee.setDependentsCount(request.dependentsCount());
        employee.setDepartment(request.department().trim());
        employee.setRole(request.role().trim().toUpperCase());
        // O salário deixou de ser editável por aqui: é uma série datada, não um número que se
        // sobrepõe. Ver B4 — sobrepô-lo perdia o valor anterior, quem o mudou, porquê e desde
        // quando, e fazia o recibo de Março passar a pagar ao valor de hoje.
        if (employee.getId() != null && employee.getBaseSalary() != null
                && request.baseSalary() != null
                && employee.getBaseSalary().compareTo(request.baseSalary()) != 0) {
            throw new BusinessRuleException(
                    "O salário não se altera na ficha do colaborador. Registe uma alteração salarial "
                            + "com data de efeito e motivo, em RH → Evolução Salarial.");
        }
        if (employee.getBaseSalary() == null) {
            employee.setBaseSalary(request.baseSalary());
        }
        employee.setHireDate(request.hireDate());
        employee.setContractEndDate(request.contractEndDate());
        employee.setBankName(blankToNull(request.bankName()));
        employee.setBankAccount(blankToNull(request.bankAccount()));
        employee.setAppUser(resolveAppUser(employee, blankToNull(request.username())));
    }

    /**
     * Resolve a conta a ligar ao colaborador. Em branco desliga a ligação (e com ela o self-service).
     * A conta tem de existir, ter acesso à empresa activa e não estar já noutro colaborador desta
     * empresa — senão "o próprio" volta a ser ambíguo, que é o que a V48 existe para resolver.
     */
    private AppUser resolveAppUser(Employee employee, String username) {
        if (username == null) {
            return null;
        }
        AppUser user = appUserService.findByUsername(username);
        if (user == null) {
            throw new BusinessRuleException("Utilizador \"" + username + "\" não existe.");
        }
        Long companyId = currentCompanyId();
        if (!user.hasCompany(companyId)) {
            throw new BusinessRuleException(
                    "O utilizador \"" + username + "\" não tem acesso a esta empresa.");
        }
        boolean taken = employee.getId() == null
                ? employeeRepository.existsByCompanyIdAndAppUserUsername(companyId, username)
                : employeeRepository.existsByCompanyIdAndAppUserUsernameAndIdNot(companyId, username, employee.getId());
        if (taken) {
            throw new BusinessRuleException(
                    "O utilizador \"" + username + "\" já está associado a outro colaborador.");
        }
        return user;
    }

    private EmployeeDTO employeeToDTO(Employee e) {
        return new EmployeeDTO(
                e.getId(), e.getEmployeeNumber(), e.getName(), e.getEmail(), e.getPhone(), e.getPhoto(),
                e.getTaxId(), e.getInssNumber(), e.getDependentsCount(), e.getDepartment(), e.getBaseSalary(), e.getRole(),
                e.getHireDate(), e.getContractEndDate(), e.getStatus(),
                e.getAppUser() != null ? e.getAppUser().getUsername() : null,
                e.getBankName(), e.getBankAccount()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.numbering.service.DocumentNumberService;
import com.phcpro.modules.numbering.service.DocumentSeries;
import com.phcpro.modules.hr.dto.AbsenceDTO;
import com.phcpro.modules.hr.dto.CreateAbsenceRequest;
import com.phcpro.modules.hr.dto.CreateExpenseClaimRequest;
import com.phcpro.modules.hr.dto.CreatePayslipRequest;
import com.phcpro.modules.hr.dto.CreateVacationRequest;
import com.phcpro.modules.hr.dto.EmployeeDTO;
import com.phcpro.modules.hr.dto.ExpenseClaimDTO;
import com.phcpro.modules.hr.dto.PayslipDTO;
import com.phcpro.modules.hr.dto.VacationDTO;
import com.phcpro.modules.hr.dto.UpsertEmployeeRequest;
import com.phcpro.modules.hr.model.Absence;
import com.phcpro.modules.hr.model.Employee;
import com.phcpro.modules.hr.model.ExpenseClaim;
import com.phcpro.modules.hr.model.ExpenseStatus;
import com.phcpro.modules.hr.model.Payslip;
import com.phcpro.modules.hr.model.Vacation;
import com.phcpro.modules.hr.repository.AbsenceRepository;
import com.phcpro.modules.hr.repository.EmployeeRepository;
import com.phcpro.modules.hr.repository.ExpenseClaimRepository;
import com.phcpro.modules.hr.repository.PayslipRepository;
import com.phcpro.modules.hr.repository.VacationRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HRService {

    /** Direito anual de férias por defeito (dias). Base: Lei do Trabalho MZ; configurável no futuro. */
    private static final int DEFAULT_ANNUAL_VACATION_DAYS = 22;

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
            @Lazy FinanceService financeService
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
    }

    @Transactional
    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        Employee employee = findEmployee(request.employeeId());

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
        Employee employee = findActiveEmployee(request.employeeId());

        if (payslipRepository.findByEmployeeIdAndYearAndMonth(employee.getId(), request.year(), request.month()).isPresent()) {
            throw new BusinessRuleException(
                    "Já existe um recibo para este colaborador em " + request.month() + "/" + request.year() + ".");
        }

        Payslip p = new Payslip();
        p.setEmployee(employee);
        p.setYear(request.year());
        p.setMonth(request.month());
        p.setBaseSalary(employee.getBaseSalary() != null ? employee.getBaseSalary() : BigDecimal.ZERO);
        p.setAllowances(orZero(request.allowances()));
        p.setOvertime(orZero(request.overtime()));
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
        auditLogService.logCurrent("PAYSLIP_ISSUE", String.format(
                "Recibo %s emitido para %s (%d/%d), líquido %s",
                saved.getPayslipNumber(), employee.getName(), request.month(), request.year(), saved.getNetPay()));
        return payslipToDTO(saved);
    }

    @Transactional
    public List<PayslipDTO> processMonthlyPayroll(int year, int month) {
        ensureHrManager();
        List<PayslipDTO> generated = employeeRepository.findByCompanyIdOrderByName(currentCompanyId()).stream()
                .filter(e -> "ACTIVE".equals(e.getStatus()))
                .filter(e -> payslipRepository.findByEmployeeIdAndYearAndMonth(e.getId(), year, month).isEmpty())
                .map(e -> createPayslip(new CreatePayslipRequest(
                        e.getId(), year, month, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        "Processamento mensal automático"
                )))
                .toList();
        auditLogService.logCurrent("PAYROLL_PROCESS", String.format(
                "Folha mensal %d/%d processada: %d recibo(s) gerado(s)", month, year, generated.size()));
        return generated;
    }

    @Transactional
    public PayslipDTO markPayslipPaid(Long id) {
        ensureHrManager();
        Payslip p = payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BusinessRuleException("Apenas recibos em rascunho podem ser marcados como pagos.");
        }
        p.setStatus("PAID");
        p.setPaymentDate(LocalDate.now());
        Payslip saved = payslipRepository.save(p);

        // Saída de tesouraria pelo líquido pago (espelho do reembolso de despesa).
        financeService.registerAutoPayout(saved.getNetPay(), String.format(
                "Pagamento Salário %s - %s", saved.getPayslipNumber(), saved.getEmployee().getName()));
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
        auditLogService.logCurrent("PAYSLIP_CANCEL", String.format(
                "Recibo %s cancelado (%s)", saved.getPayslipNumber(), saved.getEmployee().getName()));
        return payslipToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PayslipDTO> getAllPayslips() {
        return payslipRepository.findAllWithEmployeeByCompanyId(currentCompanyId()).stream()
                .map(this::payslipToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Payslip loadPayslipForPrint(Long id) {
        return payslipRepository.findByIdWithEmployeeAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
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
                .findUnjustifiedOverlapping(employee.getId(), periodStart, periodEnd).stream()
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
        return absenceToDTO(absenceRepository.save(a));
    }

    @Transactional
    public void deleteAbsence(Long id) {
        Long companyId = currentCompanyId();
        if (!absenceRepository.existsByIdAndEmployeeCompanyId(id, companyId)) {
            throw new BusinessRuleException("Falta não encontrada.");
        }
        absenceRepository.deleteByIdAndEmployeeCompanyId(id, companyId);
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
        Employee employee = findActiveEmployee(request.employeeId());
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        int requestedDays = (int) (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1);
        int reserved = vacationRepository.sumReservedDays(employee.getId(), request.yearReference());
        int remaining = DEFAULT_ANNUAL_VACATION_DAYS - reserved;
        if (requestedDays > remaining) {
            throw new BusinessRuleException(String.format(
                    "Saldo de férias insuficiente para %d: pedido de %d dia(s), disponível %d "
                            + "(direito anual %d, já reservados %d).",
                    request.yearReference(), requestedDays, Math.max(remaining, 0),
                    DEFAULT_ANNUAL_VACATION_DAYS, reserved));
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
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem executar esta operação de RH.");
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
        employee.setTaxId(blankToNull(request.taxId()));
        employee.setInssNumber(blankToNull(request.inssNumber()));
        employee.setDependentsCount(request.dependentsCount());
        employee.setDepartment(request.department().trim());
        employee.setRole(request.role().trim().toUpperCase());
        employee.setBaseSalary(request.baseSalary());
        employee.setHireDate(request.hireDate());
        employee.setContractEndDate(request.contractEndDate());
    }

    private EmployeeDTO employeeToDTO(Employee e) {
        return new EmployeeDTO(
                e.getId(), e.getEmployeeNumber(), e.getName(), e.getEmail(), e.getPhone(),
                e.getTaxId(), e.getInssNumber(), e.getDependentsCount(), e.getDepartment(), e.getBaseSalary(), e.getRole(),
                e.getHireDate(), e.getContractEndDate(), e.getStatus()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

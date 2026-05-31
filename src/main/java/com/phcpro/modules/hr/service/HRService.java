package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.hr.dto.AbsenceDTO;
import com.phcpro.modules.hr.dto.CreateAbsenceRequest;
import com.phcpro.modules.hr.dto.CreateExpenseClaimRequest;
import com.phcpro.modules.hr.dto.CreatePayslipRequest;
import com.phcpro.modules.hr.dto.CreateVacationRequest;
import com.phcpro.modules.hr.dto.EmployeeDTO;
import com.phcpro.modules.hr.dto.ExpenseClaimDTO;
import com.phcpro.modules.hr.dto.PayslipDTO;
import com.phcpro.modules.hr.dto.VacationDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HRService {

    private final EmployeeRepository employeeRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final PayslipRepository payslipRepository;
    private final AbsenceRepository absenceRepository;
    private final VacationRepository vacationRepository;
    private final ApprovalService approvalService;

    public HRService(
            EmployeeRepository employeeRepository,
            ExpenseClaimRepository expenseClaimRepository,
            PayslipRepository payslipRepository,
            AbsenceRepository absenceRepository,
            VacationRepository vacationRepository,
            @Lazy ApprovalService approvalService // Lazy injection to break potential cycles
    ) {
        this.employeeRepository = employeeRepository;
        this.expenseClaimRepository = expenseClaimRepository;
        this.payslipRepository = payslipRepository;
        this.absenceRepository = absenceRepository;
        this.vacationRepository = vacationRepository;
        this.approvalService = approvalService;
    }

    @Transactional
    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado."));

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
        return expenseClaimRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(e -> new EmployeeDTO(
                        e.getId(),
                        e.getName(),
                        e.getEmail(),
                        e.getDepartment(),
                        e.getBaseSalary(),
                        e.getRole()
                ))
                .collect(Collectors.toList());
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
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado."));

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
        p.setIrpsDeduction(orZero(request.irpsDeduction()));
        p.setInssDeduction(orZero(request.inssDeduction()));
        p.setOtherDeductions(orZero(request.otherDeductions()));
        p.setNetPay(calculateNet(p));
        p.setStatus("DRAFT");
        p.setNotes(request.notes());
        p.setPayslipNumber(generatePayslipNumber(request.year(), request.month()));

        return payslipToDTO(payslipRepository.save(p));
    }

    @Transactional
    public PayslipDTO markPayslipPaid(Long id) {
        Payslip p = payslipRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if (!"DRAFT".equals(p.getStatus())) {
            throw new BusinessRuleException("Apenas recibos em rascunho podem ser marcados como pagos.");
        }
        p.setStatus("PAID");
        p.setPaymentDate(LocalDate.now());
        return payslipToDTO(payslipRepository.save(p));
    }

    @Transactional
    public PayslipDTO cancelPayslip(Long id) {
        Payslip p = payslipRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
        if ("PAID".equals(p.getStatus())) {
            throw new BusinessRuleException("Não é possível cancelar um recibo já pago.");
        }
        p.setStatus("CANCELLED");
        return payslipToDTO(payslipRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PayslipDTO> getAllPayslips() {
        return payslipRepository.findAllWithEmployee().stream().map(this::payslipToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Payslip loadPayslipForPrint(Long id) {
        return payslipRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new BusinessRuleException("Recibo não encontrado."));
    }

    private BigDecimal calculateNet(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
        BigDecimal deductions = p.getIrpsDeduction().add(p.getInssDeduction()).add(p.getOtherDeductions());
        return gross.subtract(deductions);
    }

    private String generatePayslipNumber(int year, int month) {
        return String.format("RS-%04d%02d-%d", year, month, System.currentTimeMillis() % 100000);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private PayslipDTO payslipToDTO(Payslip p) {
        BigDecimal gross = p.getBaseSalary().add(p.getAllowances()).add(p.getOvertime());
        BigDecimal totalDeductions = p.getIrpsDeduction().add(p.getInssDeduction()).add(p.getOtherDeductions());
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
                p.getOtherDeductions(),
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
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado."));
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
        if (!absenceRepository.existsById(id)) {
            throw new BusinessRuleException("Falta não encontrada.");
        }
        absenceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AbsenceDTO> getAllAbsences() {
        return absenceRepository.findAllWithEmployee().stream().map(this::absenceToDTO).collect(Collectors.toList());
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
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado."));
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de fim não pode ser anterior à data de início.");
        }

        Vacation v = new Vacation();
        v.setEmployee(employee);
        v.setStartDate(request.startDate());
        v.setEndDate(request.endDate());
        v.setTotalDays((int) (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1));
        v.setYearReference(request.yearReference());
        v.setStatus("PENDING");
        v.setNotes(request.notes());
        return vacationToDTO(vacationRepository.save(v));
    }

    @Transactional
    public VacationDTO decideVacation(Long id, boolean approve, String decidedBy, String rejectionReason) {
        Vacation v = vacationRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Pedido de férias não encontrado."));
        if (!"PENDING".equals(v.getStatus())) {
            throw new BusinessRuleException("Apenas pedidos pendentes podem ser decididos.");
        }
        v.setStatus(approve ? "APPROVED" : "REJECTED");
        v.setDecisionBy(decidedBy);
        v.setDecisionAt(LocalDateTime.now());
        if (!approve) {
            v.setRejectionReason(rejectionReason);
        }
        return vacationToDTO(vacationRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<VacationDTO> getAllVacations() {
        return vacationRepository.findAllWithEmployee().stream().map(this::vacationToDTO).collect(Collectors.toList());
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
}

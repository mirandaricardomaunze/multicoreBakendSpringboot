package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.PayrollRunDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.AbsenceRepository;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.ExpenseClaimRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import mz.multicore.erp.modules.hr.repository.VacationRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.performance.repository.SalesGoalBonusRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HRService {

    private final HrEmployeeManagementService employeeManagementService;
    private final HrPayslipIssuanceService payslipIssuanceService;
    private final HrPayslipLifecycleService payslipLifecycleService;
    private final HrAbsenceAndVacationService absenceAndVacationService;

    @Autowired
    public HRService(
            HrEmployeeManagementService employeeManagementService,
            HrPayslipIssuanceService payslipIssuanceService,
            HrPayslipLifecycleService payslipLifecycleService,
            HrAbsenceAndVacationService absenceAndVacationService
    ) {
        this.employeeManagementService = employeeManagementService;
        this.payslipIssuanceService = payslipIssuanceService;
        this.payslipLifecycleService = payslipLifecycleService;
        this.absenceAndVacationService = absenceAndVacationService;
    }

    public HRService(
            EmployeeRepository employeeRepository, ExpenseClaimRepository expenseClaimRepository,
            PayslipRepository payslipRepository, AbsenceRepository absenceRepository,
            VacationRepository vacationRepository, HrAccessGuard guard,
            PayrollTaxService payrollTaxService, @Lazy ApprovalService approvalService,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            @Lazy FinanceService financeService, AppUserService appUserService,
            EmploymentContractService contractService, @Lazy TimeSheetService timeSheetService,
            @Lazy OvertimeValuationService overtimeValuationService, SalaryHistoryService salaryHistoryService,
            @Lazy PayrollLiabilityService payrollLiabilityService, @Lazy PayrollDeductionService payrollDeductionService,
            HrPolicyService hrPolicyService, PayrollPeriodService payrollPeriodService,
            ApplicationEventPublisher eventPublisher
    ) {
        this(employeeRepository, expenseClaimRepository, payslipRepository, absenceRepository,
                vacationRepository, guard, payrollTaxService, approvalService, documentNumberService,
                auditLogService, financeService, appUserService, contractService, timeSheetService,
                overtimeValuationService, salaryHistoryService, payrollLiabilityService,
                payrollDeductionService, hrPolicyService, payrollPeriodService, eventPublisher, null);
    }

    public HRService(
            EmployeeRepository employeeRepository, ExpenseClaimRepository expenseClaimRepository,
            PayslipRepository payslipRepository, AbsenceRepository absenceRepository,
            VacationRepository vacationRepository, HrAccessGuard guard,
            PayrollTaxService payrollTaxService, @Lazy ApprovalService approvalService,
            DocumentNumberService documentNumberService, AuditLogService auditLogService,
            @Lazy FinanceService financeService, AppUserService appUserService,
            EmploymentContractService contractService, @Lazy TimeSheetService timeSheetService,
            @Lazy OvertimeValuationService overtimeValuationService, SalaryHistoryService salaryHistoryService,
            @Lazy PayrollLiabilityService payrollLiabilityService, @Lazy PayrollDeductionService payrollDeductionService,
            HrPolicyService hrPolicyService, PayrollPeriodService payrollPeriodService,
            ApplicationEventPublisher eventPublisher, @Lazy SalesGoalBonusRepository salesGoalBonusRepository
    ) {
        this(
                new HrEmployeeManagementService(employeeRepository, expenseClaimRepository, guard, approvalService, auditLogService, appUserService),
                new HrPayslipIssuanceService(employeeRepository, payslipRepository, absenceRepository, guard, payrollTaxService,
                        documentNumberService, auditLogService, contractService, timeSheetService, overtimeValuationService,
                        salaryHistoryService, payrollDeductionService, payrollPeriodService, salesGoalBonusRepository),
                new HrPayslipLifecycleService(payslipRepository, guard, auditLogService, financeService,
                        payrollLiabilityService, payrollDeductionService, eventPublisher),
                new HrAbsenceAndVacationService(absenceRepository, vacationRepository, guard, auditLogService, timeSheetService, hrPolicyService)
        );
    }

    @Transactional
    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) { return employeeManagementService.submitExpense(request); }

    @Transactional(readOnly = true)
    public List<ExpenseClaimDTO> getAllExpenses() { return employeeManagementService.getAllExpenses(); }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() { return employeeManagementService.getAllEmployees(); }

    @Transactional
    public EmployeeDTO createEmployee(UpsertEmployeeRequest request) { return employeeManagementService.createEmployee(request); }

    @Transactional
    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest request) { return employeeManagementService.updateEmployee(id, request); }

    @Transactional
    public EmployeeDTO changeEmployeeStatus(Long id, String status) { return employeeManagementService.changeEmployeeStatus(id, status); }

    @Transactional
    public PayslipDTO createPayslip(CreatePayslipRequest request) { return payslipIssuanceService.createPayslip(request); }

    @Transactional
    public PayrollRunDTO processMonthlyPayroll(int year, int month) { return payslipIssuanceService.processMonthlyPayroll(year, month); }

    @Transactional
    public PayslipDTO approvePayslip(Long id) { return payslipLifecycleService.approvePayslip(id); }

    @Transactional
    public PayslipDTO markPayslipPaid(Long id) { return payslipLifecycleService.markPayslipPaid(id); }

    @Transactional
    public PayslipDTO cancelPayslip(Long id) { return payslipLifecycleService.cancelPayslip(id); }

    @Transactional(readOnly = true)
    public List<PayslipDTO> getAllPayslips() { return payslipLifecycleService.getAllPayslips(); }

    @Transactional(readOnly = true)
    public Payslip loadPayslipForPrint(Long id) { return payslipLifecycleService.loadPayslipForPrint(id); }

    @Transactional
    public AbsenceDTO recordAbsence(CreateAbsenceRequest request) { return absenceAndVacationService.recordAbsence(request); }

    @Transactional
    public AbsenceDTO justifyAbsence(Long id, String absenceType, String reason, boolean hasDocument) {
        return absenceAndVacationService.justifyAbsence(id, absenceType, reason, hasDocument);
    }

    @Transactional
    public void deleteAbsence(Long id) { absenceAndVacationService.deleteAbsence(id); }

    @Transactional(readOnly = true)
    public List<AbsenceDTO> getAllAbsences() { return absenceAndVacationService.getAllAbsences(); }

    @Transactional
    public VacationDTO submitVacation(CreateVacationRequest request) { return absenceAndVacationService.submitVacation(request); }

    @Transactional
    public VacationDTO decideVacation(Long id, boolean approve, String rejectionReason) {
        return absenceAndVacationService.decideVacation(id, approve, rejectionReason);
    }

    @Transactional(readOnly = true)
    public List<VacationDTO> getAllVacations() { return absenceAndVacationService.getAllVacations(); }
}

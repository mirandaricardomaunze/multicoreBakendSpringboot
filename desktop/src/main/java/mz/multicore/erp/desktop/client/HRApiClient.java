package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Fachada canónica de cliente HTTP para o módulo de Recursos Humanos no Desktop.
 * Delega para sub-clientes especializados por subdomínio de RH, preservando 100%
 * de compatibilidade retrocompatível com todos os ecrãs e testes.
 */
@Component
@Profile("desktop")
public class HRApiClient {

    private final HREmployeeApiClient employeeApiClient;
    private final HRContractApiClient contractApiClient;
    private final HRTimeSheetApiClient timeSheetApiClient;
    private final HRPayrollApiClient payrollApiClient;
    private final HRExpensesAndBenefitsApiClient expensesAndBenefitsApiClient;

    public HRApiClient(DesktopClientFactory clientFactory) {
        this(clientFactory,
             new HREmployeeApiClient(clientFactory),
             new HRContractApiClient(clientFactory),
             new HRTimeSheetApiClient(clientFactory),
             new HRPayrollApiClient(clientFactory),
             new HRExpensesAndBenefitsApiClient(clientFactory));
    }

    @Autowired
    public HRApiClient(DesktopClientFactory clientFactory,
                       HREmployeeApiClient employeeApiClient,
                       HRContractApiClient contractApiClient,
                       HRTimeSheetApiClient timeSheetApiClient,
                       HRPayrollApiClient payrollApiClient,
                       HRExpensesAndBenefitsApiClient expensesAndBenefitsApiClient) {
        this.employeeApiClient = employeeApiClient;
        this.contractApiClient = contractApiClient;
        this.timeSheetApiClient = timeSheetApiClient;
        this.payrollApiClient = payrollApiClient;
        this.expensesAndBenefitsApiClient = expensesAndBenefitsApiClient;
    }

    // ─── Funcionários & Medicina no Trabalho ─────────────────────────────────
    public List<EmployeeDTO> getAllEmployees() { return employeeApiClient.getAllEmployees(); }
    public EmployeeDTO createEmployee(UpsertEmployeeRequest req) { return employeeApiClient.createEmployee(req); }
    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest req) { return employeeApiClient.updateEmployee(id, req); }
    public EmployeeDTO changeEmployeeStatus(Long id, String status) { return employeeApiClient.changeEmployeeStatus(id, status); }
    public OccupationalHealthSummaryDTO getOccupationalHealthSummary(Long empId) { return employeeApiClient.getOccupationalHealthSummary(empId); }
    public List<OccupationalHealthExamDTO> getOccupationalHealthHistory(Long empId) { return employeeApiClient.getOccupationalHealthHistory(empId); }
    public List<OccupationalHealthExamDTO> getExpiringOccupationalHealthExams() { return employeeApiClient.getExpiringOccupationalHealthExams(); }
    public OccupationalHealthExamDTO registerOccupationalHealthExam(SaveOccupationalHealthExamRequest req) { return employeeApiClient.registerOccupationalHealthExam(req); }
    public List<HealthProviderDTO> getHealthProviders() { return employeeApiClient.getHealthProviders(); }
    public List<MissingHealthExamDTO> getEmployeesMissingHealthExam() { return employeeApiClient.getEmployeesMissingHealthExam(); }
    public OccupationalHealthCostDTO getOccupationalHealthCosts(LocalDate from, LocalDate to) { return employeeApiClient.getOccupationalHealthCosts(from, to); }
    public OccupationalHealthAttachmentDTO getOccupationalHealthAttachment(Long examId) { return employeeApiClient.getOccupationalHealthAttachment(examId); }
    public OccupationalHealthExamDTO payOccupationalHealthExam(Long examId) { return employeeApiClient.payOccupationalHealthExam(examId); }
    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long empId) { return employeeApiClient.getEmployeeDocuments(empId); }
    public List<EmployeeDocumentDTO> getExpiringEmployeeDocuments() { return employeeApiClient.getExpiringEmployeeDocuments(); }
    public EmployeeDocumentDTO saveEmployeeDocument(SaveEmployeeDocumentRequest req) { return employeeApiClient.saveEmployeeDocument(req); }
    public void deleteEmployeeDocument(Long id) { employeeApiClient.deleteEmployeeDocument(id); }

    // ─── Contratos de Trabalho ───────────────────────────────────────────────
    public List<EmploymentContractDTO> getAllContracts() { return contractApiClient.getAllContracts(); }
    public EmploymentContractDTO createContract(CreateContractRequest req) { return contractApiClient.createContract(req); }
    public EmploymentContractDTO activateContract(Long id) { return contractApiClient.activateContract(id); }
    public EmploymentContractDTO renewContract(Long id, RenewContractRequest req) { return contractApiClient.renewContract(id, req); }
    public EmploymentContractDTO terminateContract(Long id, LocalDate termDate, String reason) { return contractApiClient.terminateContract(id, termDate, reason); }
    public byte[] renderContract(Long id) { return contractApiClient.renderContract(id); }
    public ContractAlertsDTO getContractAlerts() { return contractApiClient.getContractAlerts(); }

    // ─── Ponto, Assiduidade, Faltas e Férias ─────────────────────────────────
    public TimeSheetDTO getMonthlySheet(int year, int month) { return timeSheetApiClient.getMonthlySheet(year, month); }
    public List<TimeEntryDTO> getTimeEntries(int year, int month) { return timeSheetApiClient.getTimeEntries(year, month); }
    public TimeEntryDTO recordTimeEntry(CreateTimeEntryRequest req) { return timeSheetApiClient.recordTimeEntry(req); }
    public void deleteTimeEntry(Long id) { timeSheetApiClient.deleteTimeEntry(id); }
    public TimeSheetDTO closeTimeSheet(int year, int month) { return timeSheetApiClient.closeTimeSheet(year, month); }
    public TimeSheetDTO reopenTimeSheet(int year, int month, String reason) { return timeSheetApiClient.reopenTimeSheet(year, month, reason); }
    public List<AbsenceDTO> getAllAbsences() { return timeSheetApiClient.getAllAbsences(); }
    public AbsenceDTO recordAbsence(CreateAbsenceRequest req) { return timeSheetApiClient.recordAbsence(req); }
    public void deleteAbsence(Long id) { timeSheetApiClient.deleteAbsence(id); }
    public AbsenceDTO justifyAbsence(Long id, String type, String reason, boolean hasDoc) { return timeSheetApiClient.justifyAbsence(id, type, reason, hasDoc); }
    public List<VacationDTO> getAllVacations() { return timeSheetApiClient.getAllVacations(); }
    public VacationDTO submitVacation(CreateVacationRequest req) { return timeSheetApiClient.submitVacation(req); }
    public VacationDTO decideVacation(Long id, boolean approve, String rejReason) { return timeSheetApiClient.decideVacation(id, approve, rejReason); }

    // ─── Salários, Recibos, Períodos e Subsídios Legais ──────────────────────
    public List<PayslipDTO> getAllPayslips() { return payrollApiClient.getAllPayslips(); }
    public PayslipDTO createPayslip(CreatePayslipRequest req) { return payrollApiClient.createPayslip(req); }
    public PayrollRunDTO processMonthlyPayroll(int year, int month) { return payrollApiClient.processMonthlyPayroll(year, month); }
    public PayslipDTO markPayslipPaid(Long id) { return payrollApiClient.markPayslipPaid(id); }
    public PayslipDTO markPayslipPaid(Long id, Long financeAccountId) { return payrollApiClient.markPayslipPaid(id, financeAccountId); }
    public java.util.List<mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO> treasuryAccounts() { return payrollApiClient.treasuryAccounts(); }
    public PayslipDTO approvePayslip(Long id) { return payrollApiClient.approvePayslip(id); }
    public byte[] renderPayslip(Long payslipId) { return payrollApiClient.renderPayslip(payslipId); }
    public List<PayrollPeriodDTO> getPayrollPeriods() { return payrollApiClient.getPayrollPeriods(); }
    public PayrollPeriodDTO closePayrollPeriod(int year, int month) { return payrollApiClient.closePayrollPeriod(year, month); }
    public PayrollPeriodDTO reopenPayrollPeriod(int year, int month, String reason) { return payrollApiClient.reopenPayrollPeriod(year, month, reason); }
    public BankPaymentFileDTO getBankPaymentFile(int year, int month) { return payrollApiClient.getBankPaymentFile(year, month); }
    /** /api/hr/payroll/thirteenth-month/{year} */
    public ThirteenthMonthDTO getThirteenthMonth(int year) { return payrollApiClient.getThirteenthMonth(year); }
    /** /api/hr/payroll/thirteenth-month/{year}/pay */
    public ThirteenthMonthDTO payThirteenthMonth(int year) { return payrollApiClient.payThirteenthMonth(year); }
    /** /api/hr/payroll/vacation-allowance/{vacationId} */
    public VacationAllowanceDTO getVacationAllowance(Long vacationId) { return payrollApiClient.getVacationAllowance(vacationId); }
    /** /api/hr/payroll/vacation-allowance/{vacationId}/pay */
    public VacationAllowanceDTO payVacationAllowance(Long vacationId) { return payrollApiClient.payVacationAllowance(vacationId); }
    public List<PayrollLiabilityDTO> getPayrollLiabilities() { return payrollApiClient.getPayrollLiabilities(); }
    public List<PayrollLiabilityDTO> getPayrollLiabilityAlerts() { return payrollApiClient.getPayrollLiabilityAlerts(); }
    public PayrollLiabilityDTO deliverPayrollLiability(Long id, String ref) { return payrollApiClient.deliverPayrollLiability(id, ref); }
    public List<PayrollLiabilityDTO> accruePayrollLiabilities(int year, int month) { return payrollApiClient.accruePayrollLiabilities(year, month); }
    public PayrollCostDTO getPayrollCost(int year, int month) { return payrollApiClient.getPayrollCost(year, month); }

    // ─── Despesas, Horas Extras, Políticas, Deduções e Termo ─────────────────
    public List<ExpenseClaimDTO> getAllExpenses() { return expensesAndBenefitsApiClient.getAllExpenses(); }
    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest req) { return expensesAndBenefitsApiClient.submitExpense(req); }
    public List<SalaryChangeDTO> getSalaryHistory(Long empId) { return expensesAndBenefitsApiClient.getSalaryHistory(empId); }
    public SalaryChangeDTO registerSalaryChange(CreateSalaryChangeRequest req) { return expensesAndBenefitsApiClient.registerSalaryChange(req); }
    public List<OvertimeRateConfigDTO> getOvertimeRates() { return expensesAndBenefitsApiClient.getOvertimeRates(); }
    public OvertimeRateConfigDTO saveOvertimeRates(SaveOvertimeRateConfigRequest req) { return expensesAndBenefitsApiClient.saveOvertimeRates(req); }
    public void deactivateOvertimeRates(Long id) { expensesAndBenefitsApiClient.deactivateOvertimeRates(id); }
    public List<HrPolicyConfigDTO> getHrPolicies() { return expensesAndBenefitsApiClient.getHrPolicies(); }
    public HrPolicyConfigDTO createHrPolicy(SaveHrPolicyConfigRequest req) { return expensesAndBenefitsApiClient.createHrPolicy(req); }
    public List<PayrollDeductionDTO> getDeductions() { return expensesAndBenefitsApiClient.getDeductions(); }
    public PayrollDeductionDTO createDeduction(CreatePayrollDeductionRequest req) { return expensesAndBenefitsApiClient.createDeduction(req); }
    public void deactivateDeduction(Long id) { expensesAndBenefitsApiClient.deactivateDeduction(id); }
    public List<TerminationDTO> getTerminations() { return expensesAndBenefitsApiClient.getTerminations(); }
    public TerminationDTO previewTermination(CreateTerminationRequest req) { return expensesAndBenefitsApiClient.previewTermination(req); }
    public TerminationDTO terminate(CreateTerminationRequest req) { return expensesAndBenefitsApiClient.terminate(req); }
    public TerminationDTO paySettlement(Long id) { return expensesAndBenefitsApiClient.paySettlement(id); }
    public byte[] renderSettlement(Long id) { return expensesAndBenefitsApiClient.renderSettlement(id); }
    public byte[] renderWorkCertificate(Long id) { return expensesAndBenefitsApiClient.renderWorkCertificate(id); }
}

package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthSummaryDTO;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.CreateTimeEntryRequest;
import mz.multicore.erp.modules.hr.dto.TimeEntryDTO;
import mz.multicore.erp.modules.hr.dto.TimeSheetDTO;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import mz.multicore.erp.modules.hr.dto.BankPaymentFileDTO;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.HrPolicyConfigDTO;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.PayrollPeriodDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;
import mz.multicore.erp.modules.hr.dto.OvertimeRateConfigDTO;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.dto.PayrollRunDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.dto.SaveHrPolicyConfigRequest;
import mz.multicore.erp.modules.hr.dto.SaveOvertimeRateConfigRequest;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para os Recursos Humanos ({@code /api/hr}) + impressão do recibo de salário
 * ({@code /api/print/payslip}). Espelha as assinaturas do {@code HRService} para que a migração do
 * {@code HRPanel} seja um rename. O PDF do recibo usa {@link DesktopApiClient#getBytes}.
 */
@Component
@Profile("desktop")
public class HRApiClient {

    private final DesktopClientFactory clientFactory;

    public HRApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    // ─── Funcionários ────────────────────────────────────────────────────────
    public List<EmployeeDTO> getAllEmployees() {
        return clientFactory.authenticatedClient().getList("/api/hr/employees", EmployeeDTO.class);
    }

    public EmployeeDTO createEmployee(UpsertEmployeeRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/employees", request, EmployeeDTO.class);
    }

    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest request) {
        return clientFactory.authenticatedClient().put("/api/hr/employees/" + id, request, EmployeeDTO.class);
    }

    public EmployeeDTO changeEmployeeStatus(Long id, String status) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/employees/" + id + "/status", Map.of("status", status), EmployeeDTO.class);
    }

    public OccupationalHealthSummaryDTO getOccupationalHealthSummary(Long employeeId) {
        return clientFactory.authenticatedClient().get(
                "/api/hr/occupational-health/employee/" + employeeId + "/summary",
                OccupationalHealthSummaryDTO.class);
    }

    public List<OccupationalHealthExamDTO> getOccupationalHealthHistory(Long employeeId) {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/employee/" + employeeId, OccupationalHealthExamDTO.class);
    }

    public List<OccupationalHealthExamDTO> getExpiringOccupationalHealthExams() {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/expiring", OccupationalHealthExamDTO.class);
    }

    public OccupationalHealthExamDTO registerOccupationalHealthExam(SaveOccupationalHealthExamRequest request) {
        return clientFactory.authenticatedClient().post(
                "/api/hr/occupational-health", request, OccupationalHealthExamDTO.class);
    }

    // ─── Despesas ────────────────────────────────────────────────────────────
    public List<ExpenseClaimDTO> getAllExpenses() {
        return clientFactory.authenticatedClient().getList("/api/hr/expenses", ExpenseClaimDTO.class);
    }

    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/expenses", request, ExpenseClaimDTO.class);
    }

    // ─── Recibos ─────────────────────────────────────────────────────────────
    public List<PayslipDTO> getAllPayslips() {
        return clientFactory.authenticatedClient().getList("/api/hr/payslips", PayslipDTO.class);
    }

    public PayslipDTO createPayslip(CreatePayslipRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/payslips", request, PayslipDTO.class);
    }

    // ─── Ponto e assiduidade (RH_COMPLETO_SPEC §B2) ──────────────────────────

    public TimeSheetDTO getMonthlySheet(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/timesheet/" + year + "/" + month, TimeSheetDTO.class);
    }

    public List<TimeEntryDTO> getTimeEntries(int year, int month) {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/timesheet/" + year + "/" + month + "/entries", TimeEntryDTO.class);
    }

    public TimeEntryDTO recordTimeEntry(CreateTimeEntryRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/entries", request, TimeEntryDTO.class);
    }

    public void deleteTimeEntry(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/timesheet/entries/" + id);
    }

    public TimeSheetDTO closeTimeSheet(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/" + year + "/" + month + "/close", null, TimeSheetDTO.class);
    }

    public TimeSheetDTO reopenTimeSheet(int year, int month, String reason) {
        String query = "?reason=" + java.net.URLEncoder.encode(reason, java.nio.charset.StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/" + year + "/" + month + "/reopen" + query, null, TimeSheetDTO.class);
    }

    // ─── Contratos de trabalho (RH_COMPLETO_SPEC §B1) ────────────────────────

    public List<EmploymentContractDTO> getAllContracts() {
        return clientFactory.authenticatedClient().getList("/api/hr/contracts", EmploymentContractDTO.class);
    }

    public EmploymentContractDTO createContract(CreateContractRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/contracts", request, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO activateContract(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/contracts/" + id + "/activate", null, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO renewContract(Long id, RenewContractRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/contracts/" + id + "/renew", request, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO terminateContract(Long id, LocalDate terminationDate, String reason) {
        String query = "?reason=" + java.net.URLEncoder.encode(reason, java.nio.charset.StandardCharsets.UTF_8)
                + (terminationDate == null ? "" : "&terminationDate=" + terminationDate);
        return clientFactory.authenticatedClient()
                .post("/api/hr/contracts/" + id + "/terminate" + query, null, EmploymentContractDTO.class);
    }

    /** PDF do contrato de trabalho. */
    public byte[] renderContract(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/employment-contract/" + id);
    }

    /** Fins de contrato e de período experimental à vista — uma ida ao servidor, duas listas. */
    public ContractAlertsDTO getContractAlerts() {
        return clientFactory.authenticatedClient().get("/api/hr/contracts/alerts", ContractAlertsDTO.class);
    }

    public PayrollRunDTO processMonthlyPayroll(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payslips/process/" + year + "/" + month, null, PayrollRunDTO.class);
    }

    public PayslipDTO markPayslipPaid(Long id) {
        return clientFactory.authenticatedClient().post("/api/hr/payslips/" + id + "/mark-paid", null, PayslipDTO.class);
    }

    /** PDF do recibo de salário (bytes), via endpoint de impressão já existente. */
    public byte[] renderPayslip(Long payslipId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/payslip/" + payslipId);
    }

    // ─── Faltas ──────────────────────────────────────────────────────────────
    public List<AbsenceDTO> getAllAbsences() {
        return clientFactory.authenticatedClient().getList("/api/hr/absences", AbsenceDTO.class);
    }

    public AbsenceDTO recordAbsence(CreateAbsenceRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/absences", request, AbsenceDTO.class);
    }

    public void deleteAbsence(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/absences/" + id);
    }

    // ─── Férias ──────────────────────────────────────────────────────────────
    public List<VacationDTO> getAllVacations() {
        return clientFactory.authenticatedClient().getList("/api/hr/vacations", VacationDTO.class);
    }

    public VacationDTO submitVacation(CreateVacationRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/vacations", request, VacationDTO.class);
    }

    public VacationDTO decideVacation(Long id, boolean approve, String rejectionReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("rejectionReason", rejectionReason); // pode ser null (aprovação) → HashMap aceita
        return clientFactory.authenticatedClient()
                .post("/api/hr/vacations/" + id + "/decide", body, VacationDTO.class);
    }

    /** Justificar uma falta — tipicamente uma nascida do fecho do ponto (RHC-25). */
    public AbsenceDTO justifyAbsence(Long id, String absenceType, String reason, boolean hasDocument) {
        String query = "?absenceType=" + encode(absenceType)
                + "&reason=" + encode(reason)
                + "&hasDocument=" + hasDocument;
        return clientFactory.authenticatedClient()
                .post("/api/hr/absences/" + id + "/justify" + query, null, AbsenceDTO.class);
    }

    // ─── Evolução salarial (RH_COMPLETO_SPEC §B4) ────────────────────────────

    public List<SalaryChangeDTO> getSalaryHistory(Long employeeId) {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/employees/" + employeeId + "/salary-history", SalaryChangeDTO.class);
    }

    public SalaryChangeDTO registerSalaryChange(CreateSalaryChangeRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/salary-changes", request, SalaryChangeDTO.class);
    }

    // ─── Acréscimos de hora extra (§B2 e §6) ─────────────────────────────────

    public List<OvertimeRateConfigDTO> getOvertimeRates() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/overtime-rates", OvertimeRateConfigDTO.class);
    }

    public OvertimeRateConfigDTO saveOvertimeRates(SaveOvertimeRateConfigRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/overtime-rates", request, OvertimeRateConfigDTO.class);
    }

    public void deactivateOvertimeRates(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/overtime-rates/" + id);
    }

    // ─── Retenções por entregar e valores legais (§B5 e §6) ──────────────────

    public List<PayrollLiabilityDTO> getPayrollLiabilities() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/payroll/liabilities", PayrollLiabilityDTO.class);
    }

    /** Atrasadas, a ≤7 dias do prazo, ou sem prazo configurado. Alimenta o sino. */
    public List<PayrollLiabilityDTO> getPayrollLiabilityAlerts() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/payroll/liabilities/alerts", PayrollLiabilityDTO.class);
    }

    public PayrollLiabilityDTO deliverPayrollLiability(Long id, String reference) {
        String query = reference == null || reference.isBlank() ? "" : "?reference=" + encode(reference);
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/liabilities/" + id + "/deliver" + query, null,
                        PayrollLiabilityDTO.class);
    }

    public List<PayrollLiabilityDTO> accruePayrollLiabilities(int year, int month) {
        return clientFactory.authenticatedClient()
                .postForList("/api/hr/payroll/liabilities/accrue/" + year + "/" + month, null,
                        PayrollLiabilityDTO.class);
    }

    public PayrollCostDTO getPayrollCost(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/cost/" + year + "/" + month, PayrollCostDTO.class);
    }

    public List<HrPolicyConfigDTO> getHrPolicies() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/payroll/policy", HrPolicyConfigDTO.class);
    }

    public HrPolicyConfigDTO createHrPolicy(SaveHrPolicyConfigRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/policy", request, HrPolicyConfigDTO.class);
    }

    // ─── Descontos, adiantamentos e empréstimos (§B6) ────────────────────────

    public List<PayrollDeductionDTO> getDeductions() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/deductions", PayrollDeductionDTO.class);
    }

    public PayrollDeductionDTO createDeduction(CreatePayrollDeductionRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/deductions", request, PayrollDeductionDTO.class);
    }

    public void deactivateDeduction(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/deductions/" + id);
    }

    // ─── Cessação e acerto final (§B3) ───────────────────────────────────────

    public List<TerminationDTO> getTerminations() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/terminations", TerminationDTO.class);
    }

    /** A conta antes de a cometer — cessar é irreversível. */
    public TerminationDTO previewTermination(CreateTerminationRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/terminations/preview", request, TerminationDTO.class);
    }

    public TerminationDTO terminate(CreateTerminationRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/terminations", request, TerminationDTO.class);
    }

    public TerminationDTO paySettlement(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/terminations/" + id + "/pay", null, TerminationDTO.class);
    }

    public byte[] renderSettlement(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/termination-settlement/" + id);
    }

    public byte[] renderWorkCertificate(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/work-certificate/" + id);
    }

    // ─── Correcções do §B8 ───────────────────────────────────────────────────

    /** Segunda vista antes de pagar: DRAFT → APPROVED → PAID. */
    public PayslipDTO approvePayslip(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payslips/" + id + "/approve", null, PayslipDTO.class);
    }

    public List<PayrollPeriodDTO> getPayrollPeriods() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/payroll/periods", PayrollPeriodDTO.class);
    }

    public PayrollPeriodDTO closePayrollPeriod(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/periods/" + year + "/" + month + "/close", null,
                        PayrollPeriodDTO.class);
    }

    public PayrollPeriodDTO reopenPayrollPeriod(int year, int month, String reason) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/periods/" + year + "/" + month + "/reopen?reason="
                        + encode(reason), null, PayrollPeriodDTO.class);
    }

    public BankPaymentFileDTO getBankPaymentFile(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/bank-file/" + year + "/" + month, BankPaymentFileDTO.class);
    }

    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long employeeId) {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/employee-documents/employee/" + employeeId, EmployeeDocumentDTO.class);
    }

    /** Os que caducam em breve e os que já caducaram. Alimenta o sino. */
    public List<EmployeeDocumentDTO> getExpiringEmployeeDocuments() {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/employee-documents/expiring", EmployeeDocumentDTO.class);
    }

    public EmployeeDocumentDTO saveEmployeeDocument(SaveEmployeeDocumentRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/employee-documents", request, EmployeeDocumentDTO.class);
    }

    public void deleteEmployeeDocument(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/employee-documents/" + id);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value,
                java.nio.charset.StandardCharsets.UTF_8);
    }
}

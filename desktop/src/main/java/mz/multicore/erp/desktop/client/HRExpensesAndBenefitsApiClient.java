package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP especializado para despesas, histórico salarial, horas extras, políticas, deduções e cessações.
 */
@Component
@Profile("desktop")
public class HRExpensesAndBenefitsApiClient {

    private final DesktopClientFactory clientFactory;

    public HRExpensesAndBenefitsApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ExpenseClaimDTO> getAllExpenses() {
        return clientFactory.authenticatedClient().getList("/api/hr/expenses", ExpenseClaimDTO.class);
    }

    public ExpenseClaimDTO submitExpense(CreateExpenseClaimRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/expenses", request, ExpenseClaimDTO.class);
    }

    public List<SalaryChangeDTO> getSalaryHistory(Long employeeId) {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/employees/" + employeeId + "/salary-history", SalaryChangeDTO.class);
    }

    public SalaryChangeDTO registerSalaryChange(CreateSalaryChangeRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/salary-changes", request, SalaryChangeDTO.class);
    }

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

    public List<HrPolicyConfigDTO> getHrPolicies() {
        return clientFactory.authenticatedClient().getList("/api/hr/payroll/policy", HrPolicyConfigDTO.class);
    }

    public HrPolicyConfigDTO createHrPolicy(SaveHrPolicyConfigRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/payroll/policy", request, HrPolicyConfigDTO.class);
    }

    public List<PayrollDeductionDTO> getDeductions() {
        return clientFactory.authenticatedClient().getList("/api/hr/deductions", PayrollDeductionDTO.class);
    }

    public PayrollDeductionDTO createDeduction(CreatePayrollDeductionRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/deductions", request, PayrollDeductionDTO.class);
    }

    public void deactivateDeduction(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/deductions/" + id);
    }

    public List<TerminationDTO> getTerminations() {
        return clientFactory.authenticatedClient().getList("/api/hr/terminations", TerminationDTO.class);
    }

    public TerminationDTO previewTermination(CreateTerminationRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/terminations/preview", request, TerminationDTO.class);
    }

    public TerminationDTO terminate(CreateTerminationRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/terminations", request, TerminationDTO.class);
    }

    public TerminationDTO paySettlement(Long id) {
        return clientFactory.authenticatedClient().post("/api/hr/terminations/" + id + "/pay", null, TerminationDTO.class);
    }

    public byte[] renderSettlement(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/termination-settlement/" + id);
    }

    public byte[] renderWorkCertificate(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/work-certificate/" + id);
    }
}

package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP especializado para processamento salarial, recibos, 13º mês e obrigações legais.
 */
@Component
@Profile("desktop")
public class HRPayrollApiClient {

    private final DesktopClientFactory clientFactory;

    public HRPayrollApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<PayslipDTO> getAllPayslips() {
        return clientFactory.authenticatedClient().getList("/api/hr/payslips", PayslipDTO.class);
    }

    public PayslipDTO createPayslip(CreatePayslipRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/payslips", request, PayslipDTO.class);
    }

    public PayrollRunDTO processMonthlyPayroll(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payslips/process/" + year + "/" + month, null, PayrollRunDTO.class);
    }

    public PayslipDTO markPayslipPaid(Long id) {
        return clientFactory.authenticatedClient().post("/api/hr/payslips/" + id + "/mark-paid", null, PayslipDTO.class);
    }

    public PayslipDTO approvePayslip(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payslips/" + id + "/approve", null, PayslipDTO.class);
    }

    public byte[] renderPayslip(Long payslipId) {
        return clientFactory.authenticatedClient().getBytes("/api/print/payslip/" + payslipId);
    }

    public List<PayrollPeriodDTO> getPayrollPeriods() {
        return clientFactory.authenticatedClient().getList("/api/hr/payroll/periods", PayrollPeriodDTO.class);
    }

    public PayrollPeriodDTO closePayrollPeriod(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/periods/" + year + "/" + month + "/close", null, PayrollPeriodDTO.class);
    }

    public PayrollPeriodDTO reopenPayrollPeriod(int year, int month, String reason) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/periods/" + year + "/" + month + "/reopen?reason="
                        + URLEncoder.encode(reason, StandardCharsets.UTF_8), null, PayrollPeriodDTO.class);
    }

    public BankPaymentFileDTO getBankPaymentFile(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/bank-file/" + year + "/" + month, BankPaymentFileDTO.class);
    }

    public ThirteenthMonthDTO getThirteenthMonth(int year) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/thirteenth-month/" + year, ThirteenthMonthDTO.class);
    }

    public ThirteenthMonthDTO payThirteenthMonth(int year) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/thirteenth-month/" + year + "/pay", null, ThirteenthMonthDTO.class);
    }

    public VacationAllowanceDTO getVacationAllowance(Long vacationId) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/vacation-allowance/" + vacationId, VacationAllowanceDTO.class);
    }

    public VacationAllowanceDTO payVacationAllowance(Long vacationId) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/payroll/vacation-allowance/" + vacationId + "/pay", null, VacationAllowanceDTO.class);
    }

    public List<PayrollLiabilityDTO> getPayrollLiabilities() {
        return clientFactory.authenticatedClient().getList("/api/hr/payroll/liabilities", PayrollLiabilityDTO.class);
    }

    public List<PayrollLiabilityDTO> getPayrollLiabilityAlerts() {
        return clientFactory.authenticatedClient().getList("/api/hr/payroll/liabilities/alerts", PayrollLiabilityDTO.class);
    }

    public PayrollLiabilityDTO deliverPayrollLiability(Long id, String reference) {
        String query = reference == null || reference.isBlank() ? "" : "?reference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient().post("/api/hr/payroll/liabilities/" + id + "/deliver" + query, null, PayrollLiabilityDTO.class);
    }

    public List<PayrollLiabilityDTO> accruePayrollLiabilities(int year, int month) {
        return clientFactory.authenticatedClient()
                .postForList("/api/hr/payroll/liabilities/accrue/" + year + "/" + month, null, PayrollLiabilityDTO.class);
    }

    public PayrollCostDTO getPayrollCost(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/payroll/cost/" + year + "/" + month, PayrollCostDTO.class);
    }
}

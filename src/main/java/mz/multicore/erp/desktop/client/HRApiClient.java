package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateAbsenceRequest;
import mz.multicore.erp.modules.hr.dto.CreateExpenseClaimRequest;
import mz.multicore.erp.modules.hr.dto.CreatePayslipRequest;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.UpsertEmployeeRequest;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

    public List<PayslipDTO> processMonthlyPayroll(int year, int month) {
        return clientFactory.authenticatedClient()
                .postForList("/api/hr/payslips/process/" + year + "/" + month, null, PayslipDTO.class);
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
}

package com.phcpro.modules.hr.controller;

import com.phcpro.modules.hr.dto.AbsenceDTO;
import com.phcpro.modules.hr.dto.CreateAbsenceRequest;
import com.phcpro.modules.hr.dto.CreateExpenseClaimRequest;
import com.phcpro.modules.hr.dto.CreatePayslipRequest;
import com.phcpro.modules.hr.dto.CreateVacationRequest;
import com.phcpro.modules.hr.dto.EmployeeDTO;
import com.phcpro.modules.hr.dto.ExpenseClaimDTO;
import com.phcpro.modules.hr.dto.PayrollFiscalSummaryDTO;
import com.phcpro.modules.hr.dto.PayrollTaxConfigDTO;
import com.phcpro.modules.hr.dto.PayslipDTO;
import com.phcpro.modules.hr.dto.SavePayrollTaxConfigRequest;
import com.phcpro.modules.hr.dto.ThirteenthMonthDTO;
import com.phcpro.modules.hr.dto.VacationAllowanceDTO;
import com.phcpro.modules.hr.dto.VacationDTO;
import com.phcpro.modules.hr.dto.UpsertEmployeeRequest;
import com.phcpro.modules.hr.service.HRService;
import com.phcpro.modules.hr.service.PayrollBonusService;
import com.phcpro.modules.hr.service.PayrollTaxConfigService;
import com.phcpro.modules.hr.service.PayrollTaxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
public class HRController {

    private final HRService hrService;
    private final PayrollTaxService payrollTaxService;
    private final PayrollTaxConfigService payrollTaxConfigService;
    private final PayrollBonusService payrollBonusService;

    public HRController(
            HRService hrService,
            PayrollTaxService payrollTaxService,
            PayrollTaxConfigService payrollTaxConfigService,
            PayrollBonusService payrollBonusService
    ) {
        this.hrService = hrService;
        this.payrollTaxService = payrollTaxService;
        this.payrollTaxConfigService = payrollTaxConfigService;
        this.payrollBonusService = payrollBonusService;
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDTO>> getEmployees() {
        return ResponseEntity.ok(hrService.getAllEmployees());
    }

    @PostMapping("/employees")
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody @Valid UpsertEmployeeRequest request) {
        return ResponseEntity.ok(hrService.createEmployee(request));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @RequestBody @Valid UpsertEmployeeRequest request
    ) {
        return ResponseEntity.ok(hrService.updateEmployee(id, request));
    }

    @PostMapping("/employees/{id}/status")
    public ResponseEntity<EmployeeDTO> changeEmployeeStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(hrService.changeEmployeeStatus(id, body.get("status")));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseClaimDTO>> getExpenses() {
        return ResponseEntity.ok(hrService.getAllExpenses());
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseClaimDTO> submitExpense(@RequestBody @Valid CreateExpenseClaimRequest request) {
        return ResponseEntity.ok(hrService.submitExpense(request));
    }

    // ─── Payslips ──────────────────────────────────────────────────────────

    @GetMapping("/payslips")
    public ResponseEntity<List<PayslipDTO>> getPayslips() {
        return ResponseEntity.ok(hrService.getAllPayslips());
    }

    @PostMapping("/payslips")
    public ResponseEntity<PayslipDTO> createPayslip(@RequestBody @Valid CreatePayslipRequest request) {
        return ResponseEntity.ok(hrService.createPayslip(request));
    }

    @PostMapping("/payslips/process/{year}/{month}")
    public ResponseEntity<List<PayslipDTO>> processPayroll(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(hrService.processMonthlyPayroll(year, month));
    }

    @PostMapping("/payslips/{id}/mark-paid")
    public ResponseEntity<PayslipDTO> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(hrService.markPayslipPaid(id));
    }

    @PostMapping("/payslips/{id}/cancel")
    public ResponseEntity<PayslipDTO> cancelPayslip(@PathVariable Long id) {
        return ResponseEntity.ok(hrService.cancelPayslip(id));
    }

    // ─── Absences ──────────────────────────────────────────────────────────

    @GetMapping("/absences")
    public ResponseEntity<List<AbsenceDTO>> getAbsences() {
        return ResponseEntity.ok(hrService.getAllAbsences());
    }

    @PostMapping("/absences")
    public ResponseEntity<AbsenceDTO> recordAbsence(@RequestBody @Valid CreateAbsenceRequest request) {
        return ResponseEntity.ok(hrService.recordAbsence(request));
    }

    @DeleteMapping("/absences/{id}")
    public ResponseEntity<Void> deleteAbsence(@PathVariable Long id) {
        hrService.deleteAbsence(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Vacations ─────────────────────────────────────────────────────────

    @GetMapping("/vacations")
    public ResponseEntity<List<VacationDTO>> getVacations() {
        return ResponseEntity.ok(hrService.getAllVacations());
    }

    @PostMapping("/vacations")
    public ResponseEntity<VacationDTO> submitVacation(@RequestBody @Valid CreateVacationRequest request) {
        return ResponseEntity.ok(hrService.submitVacation(request));
    }

    @PostMapping("/vacations/{id}/decide")
    public ResponseEntity<VacationDTO> decideVacation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        String rejectionReason = (String) body.get("rejectionReason");
        // O decisor é resolvido no Service a partir do utilizador autenticado, não do corpo do pedido.
        return ResponseEntity.ok(hrService.decideVacation(id, approve, rejectionReason));
    }

    // ─── Mapa fiscal salarial (INSS/IRPS) ────────────────────────────────────

    @GetMapping("/payroll/fiscal-summary/{year}/{month}")
    public ResponseEntity<PayrollFiscalSummaryDTO> fiscalSummary(
            @PathVariable int year,
            @PathVariable int month
    ) {
        return ResponseEntity.ok(payrollTaxService.fiscalSummary(year, month));
    }

    // ─── Configuração fiscal salarial (escalões IRPS / taxas INSS) ───────────

    @GetMapping("/payroll/tax-config")
    public ResponseEntity<List<PayrollTaxConfigDTO>> taxConfigs() {
        return ResponseEntity.ok(payrollTaxConfigService.list());
    }

    @PostMapping("/payroll/tax-config")
    public ResponseEntity<PayrollTaxConfigDTO> createTaxConfig(
            @RequestBody @Valid SavePayrollTaxConfigRequest request
    ) {
        return ResponseEntity.ok(payrollTaxConfigService.create(request));
    }

    // ─── Subsídios legais (13.º mês / subsídio de férias) ────────────────────

    @GetMapping("/payroll/thirteenth-month/{year}")
    public ResponseEntity<ThirteenthMonthDTO> thirteenthMonth(@PathVariable int year) {
        return ResponseEntity.ok(payrollBonusService.thirteenthMonth(year));
    }

    @GetMapping("/payroll/vacation-allowance/{vacationId}")
    public ResponseEntity<VacationAllowanceDTO> vacationAllowance(@PathVariable Long vacationId) {
        return ResponseEntity.ok(payrollBonusService.vacationAllowance(vacationId));
    }

    @PostMapping("/payroll/thirteenth-month/{year}/pay")
    public ResponseEntity<ThirteenthMonthDTO> payThirteenthMonth(@PathVariable int year) {
        return ResponseEntity.ok(payrollBonusService.payThirteenthMonth(year));
    }

    @PostMapping("/payroll/vacation-allowance/{vacationId}/pay")
    public ResponseEntity<VacationAllowanceDTO> payVacationAllowance(@PathVariable Long vacationId) {
        return ResponseEntity.ok(payrollBonusService.payVacationAllowance(vacationId));
    }
}

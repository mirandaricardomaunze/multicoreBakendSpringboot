package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.BankPaymentFileDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.PayrollPeriodDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;
import mz.multicore.erp.modules.hr.service.BankPaymentFileService;
import mz.multicore.erp.modules.hr.service.EmployeeDocumentService;
import mz.multicore.erp.modules.hr.service.PayrollPeriodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * As correcções do §B8 que precisam de porta HTTP: fecho do mês da folha, ficheiro de pagamento
 * bancário e documentos do colaborador. Ver docs/RH_COMPLETO_SPEC.md §B8.
 */
@RestController
@RequestMapping("/api/hr")
public class PayrollPeriodController {

    private final PayrollPeriodService payrollPeriodService;
    private final BankPaymentFileService bankPaymentFileService;
    private final EmployeeDocumentService employeeDocumentService;

    public PayrollPeriodController(PayrollPeriodService payrollPeriodService,
                                   BankPaymentFileService bankPaymentFileService,
                                   EmployeeDocumentService employeeDocumentService) {
        this.payrollPeriodService = payrollPeriodService;
        this.bankPaymentFileService = bankPaymentFileService;
        this.employeeDocumentService = employeeDocumentService;
    }

    // ─── Fecho do mês da folha (§B8.6) ────────────────────────────────────────

    @GetMapping("/payroll/periods")
    public ResponseEntity<List<PayrollPeriodDTO>> periods() {
        return ResponseEntity.ok(payrollPeriodService.list());
    }

    @PostMapping("/payroll/periods/{year}/{month}/close")
    public ResponseEntity<PayrollPeriodDTO> close(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(payrollPeriodService.close(year, month));
    }

    @PostMapping("/payroll/periods/{year}/{month}/reopen")
    public ResponseEntity<PayrollPeriodDTO> reopen(@PathVariable int year, @PathVariable int month,
                                                   @RequestParam String reason) {
        return ResponseEntity.ok(payrollPeriodService.reopen(year, month, reason));
    }

    // ─── Ficheiro de pagamento bancário (§B8.7) ───────────────────────────────

    @GetMapping("/payroll/bank-file/{year}/{month}")
    public ResponseEntity<BankPaymentFileDTO> bankFile(@PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(bankPaymentFileService.generate(year, month));
    }

    // ─── Documentos do colaborador (§B8.8) ────────────────────────────────────

    @GetMapping("/employee-documents")
    public ResponseEntity<List<EmployeeDocumentDTO>> documents() {
        return ResponseEntity.ok(employeeDocumentService.list());
    }

    @GetMapping("/employee-documents/employee/{employeeId}")
    public ResponseEntity<List<EmployeeDocumentDTO>> documentsOf(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeDocumentService.listForEmployee(employeeId));
    }

    /** Os que caducam em breve e os que já caducaram. Alimenta o sino. */
    @GetMapping("/employee-documents/expiring")
    public ResponseEntity<List<EmployeeDocumentDTO>> expiringDocuments() {
        return ResponseEntity.ok(employeeDocumentService.expiringSoon());
    }

    @PostMapping("/employee-documents")
    public ResponseEntity<EmployeeDocumentDTO> saveDocument(
            @RequestBody @Valid SaveEmployeeDocumentRequest request) {
        return ResponseEntity.ok(employeeDocumentService.save(request));
    }

    @DeleteMapping("/employee-documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        employeeDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

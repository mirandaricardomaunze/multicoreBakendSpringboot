package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDeductionLineDTO;
import mz.multicore.erp.modules.hr.service.PayrollDeductionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Descontos recorrentes, adiantamentos e empréstimos. Ver docs/RH_COMPLETO_SPEC.md §B6. */
@RestController
@RequestMapping("/api/hr/deductions")
public class PayrollDeductionController {

    private final PayrollDeductionService deductionService;

    public PayrollDeductionController(PayrollDeductionService deductionService) {
        this.deductionService = deductionService;
    }

    @GetMapping
    public ResponseEntity<List<PayrollDeductionDTO>> list() {
        return ResponseEntity.ok(deductionService.list());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollDeductionDTO>> ofEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(deductionService.listForEmployee(employeeId));
    }

    /** O que o colaborador ainda deve — é o que entra no acerto final (B3). */
    @GetMapping("/employee/{employeeId}/outstanding")
    public ResponseEntity<List<PayrollDeductionDTO>> outstanding(@PathVariable Long employeeId) {
        return ResponseEntity.ok(deductionService.outstandingFor(employeeId));
    }

    @PostMapping
    public ResponseEntity<PayrollDeductionDTO> create(
            @RequestBody @Valid CreatePayrollDeductionRequest request) {
        return ResponseEntity.ok(deductionService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deductionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** As linhas discriminadas de um recibo — o que substitui o total anónimo (RHC-63). */
    @GetMapping("/payslip/{payslipId}/lines")
    public ResponseEntity<List<PayslipDeductionLineDTO>> linesOfPayslip(@PathVariable Long payslipId) {
        return ResponseEntity.ok(deductionService.linesOf(payslipId));
    }
}

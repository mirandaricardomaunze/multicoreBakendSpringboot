package mz.multicore.erp.modules.comercial.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;
import mz.multicore.erp.modules.comercial.service.InvoiceInstallmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/comercial/invoices/{invoiceId}/installments")
public class InvoiceInstallmentController {

    private final InvoiceInstallmentService installmentService;
    private final mz.multicore.erp.modules.comercial.service.InvoiceSettlementService settlementService;

    public InvoiceInstallmentController(InvoiceInstallmentService installmentService,
                                        mz.multicore.erp.modules.comercial.service.InvoiceSettlementService settlementService) {
        this.installmentService = installmentService;
        this.settlementService = settlementService;
    }

    @PostMapping
    public ResponseEntity<List<InvoiceInstallmentDTO>> createPlan(
            @PathVariable Long invoiceId,
            @RequestBody @Valid CreateInstallmentPlanRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        // Garante que o invoiceId do path coincide com a requisição
        CreateInstallmentPlanRequest effective = new CreateInstallmentPlanRequest(
                invoiceId,
                request.numberOfInstallments(),
                request.intervalDays(),
                request.firstDueDate(),
                request.customAmounts()
        );
        return ResponseEntity.ok(installmentService.createInstallmentPlan(companyId, effective));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceInstallmentDTO>> getInstallments(@PathVariable Long invoiceId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(installmentService.getInstallments(companyId, invoiceId));
    }

    @PostMapping("/pay")
    public ResponseEntity<List<InvoiceInstallmentDTO>> applyPayment(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam Long financeAccountId) {
        // The money enters through a receipt; the plan follows it (InstallmentReceiptListener).
        settlementService.settle(invoiceId, financeAccountId, amount);
        return ResponseEntity.ok(installmentService.getInstallments(CurrentUserContext.getCurrentCompanyId(), invoiceId));
    }
}

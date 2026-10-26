package mz.multicore.erp.modules.comercial.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import mz.multicore.erp.modules.comercial.service.SalesReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Controller REST para o assistente de devoluções no backoffice com reintegração FEFO.
 */
@RestController
@RequestMapping("/api/comercial/invoices")
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    public SalesReturnController(SalesReturnService salesReturnService) {
        this.salesReturnService = Objects.requireNonNull(salesReturnService, "salesReturnService obrigatório");
    }

    @GetMapping("/{id}/returnable-lines")
    public ResponseEntity<List<ReturnableInvoiceLineDTO>> getReturnableLines(@PathVariable Long id) {
        return ResponseEntity.ok(salesReturnService.getReturnableLines(id));
    }

    @PostMapping("/{id}/sales-return")
    public ResponseEntity<SalesReturnWizardResultDTO> processReturn(
            @PathVariable Long id,
            @Valid @RequestBody SalesReturnWizardRequest request) {
        return ResponseEntity.ok(salesReturnService.processReturn(request));
    }
}

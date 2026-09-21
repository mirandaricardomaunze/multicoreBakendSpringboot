package mz.multicore.erp.modules.purchases.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.printing.SupplierStatementPrintService;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.service.SupplierStatementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints REST para consulta e emissão em PDF de Extratos de Conta Corrente de Fornecedores.
 */
@RestController
@RequestMapping("/api/purchases/statements")
public class SupplierStatementController {

    private final SupplierStatementService statementService;
    private final SupplierStatementPrintService printService;

    public SupplierStatementController(
            SupplierStatementService statementService,
            SupplierStatementPrintService printService
    ) {
        this.statementService = statementService;
        this.printService = printService;
    }

    @GetMapping("/supplier")
    public ResponseEntity<SupplierStatementDTO> getSupplierStatement(
            @RequestParam Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(statementService.getStatement(supplierId, startDate, endDate));
    }

    @GetMapping("/supplier/pdf")
    public ResponseEntity<byte[]> getSupplierStatementPdf(
            @RequestParam Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = printService.render(companyId, supplierId, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"extrato-fornecedor-" + supplierId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

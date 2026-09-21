package mz.multicore.erp.modules.comercial.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;
import mz.multicore.erp.modules.comercial.dto.SendStatementEmailRequest;
import mz.multicore.erp.modules.comercial.service.CustomerStatementMailService;
import mz.multicore.erp.modules.comercial.service.CustomerStatementService;
import mz.multicore.erp.modules.printing.CustomerStatementPrintService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints REST para consulta e emissão em PDF de Extratos de Conta Corrente de Clientes.
 */
@RestController
@RequestMapping("/api/comercial/statements")
public class CustomerStatementController {

    private final CustomerStatementService statementService;
    private final CustomerStatementPrintService printService;
    private final CustomerStatementMailService mailService;

    public CustomerStatementController(
            CustomerStatementService statementService,
            CustomerStatementPrintService printService,
            CustomerStatementMailService mailService
    ) {
        this.statementService = statementService;
        this.printService = printService;
        this.mailService = mailService;
    }

    @GetMapping("/customer")
    public ResponseEntity<CustomerStatementDTO> getCustomerStatement(
            @RequestParam Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(statementService.getStatement(clientId, startDate, endDate));
    }

    @GetMapping("/customer/pdf")
    public ResponseEntity<byte[]> getCustomerStatementPdf(
            @RequestParam Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        byte[] pdfBytes = printService.render(companyId, clientId, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"extrato-cliente-" + clientId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/customer/email")
    public ResponseEntity<EmailDispatchResultDTO> sendCustomerStatementEmail(
            @Valid @RequestBody SendStatementEmailRequest request
    ) {
        return ResponseEntity.ok(mailService.sendStatementEmail(request));
    }
}

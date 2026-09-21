package mz.multicore.erp.modules.pos.controller;

import mz.multicore.erp.architecture.concurrency.ConcurrencyRetry;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.service.POSService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
public class POSController {

    private final POSService posService;
    private final ComercialService comercialService;
    private final ConcurrencyRetry concurrencyRetry;
    private final mz.multicore.erp.modules.printing.POSZReportPrintService poszReportPrintService;

    public POSController(POSService posService, ComercialService comercialService,
                         ConcurrencyRetry concurrencyRetry,
                         mz.multicore.erp.modules.printing.POSZReportPrintService poszReportPrintService) {
        this.posService = posService;
        this.comercialService = comercialService;
        this.concurrencyRetry = concurrencyRetry;
        this.poszReportPrintService = poszReportPrintService;
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<TillSessionDTO> getActiveSession(@RequestParam String operator, @RequestParam Long companyId) {
        return posService.getActiveSession(operator, companyId)
                .map(posService::toDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<TillSessionDTO>> getSessionsByCompany(@RequestParam Long companyId) {
        List<TillSessionDTO> sessions = posService.getSessionsByCompany(companyId)
                .stream().map(posService::toDTO).toList();
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/sessions/open")
    public ResponseEntity<TillSessionDTO> openSession(@RequestBody @Valid OpenSessionRequest request) {
        return ResponseEntity.ok(posService.toDTO(
                posService.openSession(request.operator(), request.openingBalance(), request.companyId())));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<TillSessionDTO> closeSession(
            @PathVariable Long sessionId,
            @RequestBody @Valid CloseSessionRequest request
    ) {
        return ResponseEntity.ok(posService.toDTO(
                posService.closeSession(sessionId, request.closingBalanceReal(), request.depositAccountId())));
    }

    @GetMapping("/sessions/{sessionId}/movements")
    public ResponseEntity<List<TillMovementDTO>> getMovements(@PathVariable Long sessionId) {
        List<TillMovementDTO> movements = posService.getMovementsBySession(sessionId)
                .stream().map(posService::toDTO).toList();
        return ResponseEntity.ok(movements);
    }

    @PostMapping("/sessions/{sessionId}/movements")
    public ResponseEntity<TillMovementDTO> addMovement(
            @PathVariable Long sessionId,
            @RequestBody @Valid CashMovementRequest request
    ) {
        return ResponseEntity.ok(posService.toDTO(
                posService.addCashMovement(sessionId, request.type(), request.amount(), request.description())));
    }

    @PostMapping("/checkout")
    public ResponseEntity<InvoiceDTO> checkout(@RequestBody @Valid POSCheckoutRequest request) {
        // Rede de segurança: se dois postos colidirem na MESMA linha (stock/tesouraria) no mesmo
        // instante, repete a venda numa transação nova em vez de falhar na cara do operador.
        return ResponseEntity.ok(concurrencyRetry.run(() -> comercialService.toDTO(posService.checkout(request))));
    }

    @PostMapping("/returns")
    public ResponseEntity<CreditNoteDTO> returnSale(@RequestBody @Valid POSReturnRequest request) {
        return ResponseEntity.ok(posService.returnSale(request));
    }

    /** Regista um pagamento posterior (fiado) sobre uma fatura em dívida. */
    @PostMapping("/invoices/{invoiceId}/late-payment")
    public ResponseEntity<Void> registerLatePayment(@PathVariable Long invoiceId,
                                                    @RequestBody @Valid PosPaymentRequest request) {
        posService.registerLatePayment(invoiceId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/z-report")
    public ResponseEntity<PosZReportDTO> getZReport(@PathVariable Long sessionId) {
        return ResponseEntity.ok(posService.buildZReport(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/z-report/pdf")
    public ResponseEntity<byte[]> getZReportPdf(@PathVariable Long sessionId) {
        byte[] pdf = poszReportPrintService.render(sessionId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"relatorio_z_" + sessionId + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/sessions/history")
    public ResponseEntity<List<mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO>> getSessionsHistory(
            @RequestParam Long companyId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to
    ) {
        return ResponseEntity.ok(posService.getSessionsHistory(companyId, from, to));
    }
}

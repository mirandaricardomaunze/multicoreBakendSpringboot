package com.phcpro.modules.pos.controller;

import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.InvoiceDTO;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.pos.dto.*;
import com.phcpro.modules.pos.service.POSService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos")
public class POSController {

    private final POSService posService;
    private final ComercialService comercialService;

    public POSController(POSService posService, ComercialService comercialService) {
        this.posService = posService;
        this.comercialService = comercialService;
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
        return ResponseEntity.ok(comercialService.toDTO(posService.checkout(request)));
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
}

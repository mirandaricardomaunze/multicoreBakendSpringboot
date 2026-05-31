package com.phcpro.modules.pos.controller;

import com.phcpro.modules.comercial.model.Invoice;
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

    public POSController(POSService posService) {
        this.posService = posService;
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
                posService.closeSession(sessionId, request.closingBalanceReal())));
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
    public ResponseEntity<Long> checkout(@RequestBody @Valid POSCheckoutRequest request) {
        Invoice invoice = posService.checkout(request);
        return ResponseEntity.ok(invoice.getId());
    }
}

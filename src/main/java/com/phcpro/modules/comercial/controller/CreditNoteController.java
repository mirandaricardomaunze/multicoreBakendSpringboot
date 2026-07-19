package com.phcpro.modules.comercial.controller;

import com.phcpro.modules.comercial.dto.CreateCreditNoteRequest;
import com.phcpro.modules.comercial.dto.CreditNoteDTO;
import com.phcpro.modules.comercial.dto.ReturnedQtyDTO;
import com.phcpro.modules.comercial.service.CreditNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-notes")
public class CreditNoteController {

    private final CreditNoteService service;

    public CreditNoteController(CreditNoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CreditNoteDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.findByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditNoteDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** Quantidades já devolvidas por linha de uma fatura (para limitar novas devoluções). */
    @GetMapping("/returned-quantities")
    public ResponseEntity<List<ReturnedQtyDTO>> returnedQuantities(@RequestParam Long invoiceId) {
        List<ReturnedQtyDTO> result = service.getReturnedQuantitiesByInvoiceLine(invoiceId)
                .entrySet().stream()
                .map(e -> new ReturnedQtyDTO(e.getKey(), e.getValue()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<CreditNoteDTO> create(@RequestBody @Valid CreateCreditNoteRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CreditNoteDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CreditNoteDTO> reject(@PathVariable Long id,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("rejectionReason");
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CreditNoteDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}

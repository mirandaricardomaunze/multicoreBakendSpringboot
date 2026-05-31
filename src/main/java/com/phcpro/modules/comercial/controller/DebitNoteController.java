package com.phcpro.modules.comercial.controller;

import com.phcpro.modules.comercial.dto.CreateDebitNoteRequest;
import com.phcpro.modules.comercial.dto.DebitNoteDTO;
import com.phcpro.modules.comercial.service.DebitNoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debit-notes")
public class DebitNoteController {

    private final DebitNoteService service;

    public DebitNoteController(DebitNoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<DebitNoteDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.findByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DebitNoteDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<DebitNoteDTO> create(@RequestBody @Valid CreateDebitNoteRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DebitNoteDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DebitNoteDTO> reject(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("rejectionReason");
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<DebitNoteDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}

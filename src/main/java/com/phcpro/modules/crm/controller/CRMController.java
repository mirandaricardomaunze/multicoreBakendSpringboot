package com.phcpro.modules.crm.controller;

import com.phcpro.modules.crm.dto.*;
import com.phcpro.modules.crm.service.CRMService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm")
public class CRMController {

    private final CRMService crmService;

    public CRMController(CRMService crmService) {
        this.crmService = crmService;
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketDTO>> getTickets() {
        return ResponseEntity.ok(crmService.getAllTickets());
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketDTO> createTicket(@RequestBody @Valid CreateTicketRequest request) {
        return ResponseEntity.ok(crmService.createTicket(request));
    }

    @GetMapping("/worksheets")
    public ResponseEntity<List<WorkSheetDTO>> getWorkSheets() {
        return ResponseEntity.ok(crmService.getAllWorkSheets());
    }

    @PostMapping("/worksheets")
    public ResponseEntity<WorkSheetDTO> createWorkSheet(@RequestBody @Valid CreateWorkSheetRequest request) {
        return ResponseEntity.ok(crmService.createWorkSheet(request));
    }

    @PostMapping("/worksheets/{id}/bill")
    public ResponseEntity<Void> billWorkSheet(@PathVariable Long id) {
        crmService.billWorkSheet(id);
        return ResponseEntity.ok().build();
    }
}

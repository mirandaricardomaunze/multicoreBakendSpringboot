package mz.multicore.erp.modules.crm.controller;

import mz.multicore.erp.modules.crm.dto.*;
import mz.multicore.erp.modules.crm.service.CRMService;
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

    @GetMapping("/settings")
    public ResponseEntity<CrmSettingsDTO> getSettings() {
        return ResponseEntity.ok(crmService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<CrmSettingsDTO> updateSettings(@RequestBody @Valid UpdateCrmSettingsRequest request) {
        return ResponseEntity.ok(crmService.updateSettings(request));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicketDTO>> getTickets() {
        return ResponseEntity.ok(crmService.getAllTickets());
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketDTO> createTicket(@RequestBody @Valid CreateTicketRequest request) {
        return ResponseEntity.ok(crmService.createTicket(request));
    }

    @PutMapping("/tickets/{id}")
    public ResponseEntity<SupportTicketDTO> updateTicket(
            @PathVariable Long id, @RequestBody @Valid UpdateTicketRequest request) {
        return ResponseEntity.ok(crmService.updateTicket(id, request));
    }

    @PostMapping("/tickets/{id}/status")
    public ResponseEntity<SupportTicketDTO> changeTicketStatus(
            @PathVariable Long id, @RequestBody @Valid ChangeTicketStatusRequest request) {
        return ResponseEntity.ok(crmService.changeTicketStatus(id, request));
    }

    @GetMapping("/worksheets")
    public ResponseEntity<List<WorkSheetDTO>> getWorkSheets() {
        return ResponseEntity.ok(crmService.getAllWorkSheets());
    }

    @PostMapping("/worksheets")
    public ResponseEntity<WorkSheetDTO> createWorkSheet(@RequestBody @Valid CreateWorkSheetRequest request) {
        return ResponseEntity.ok(crmService.createWorkSheet(request));
    }

    @PutMapping("/worksheets/{id}")
    public ResponseEntity<WorkSheetDTO> updateWorkSheet(
            @PathVariable Long id, @RequestBody @Valid UpdateWorkSheetRequest request) {
        return ResponseEntity.ok(crmService.updateWorkSheet(id, request));
    }

    @PostMapping("/worksheets/{id}/void")
    public ResponseEntity<WorkSheetDTO> voidWorkSheet(
            @PathVariable Long id, @RequestBody @Valid VoidWorkSheetRequest request) {
        return ResponseEntity.ok(crmService.voidWorkSheet(id, request));
    }

    @PostMapping("/worksheets/{id}/bill")
    public ResponseEntity<Void> billWorkSheet(@PathVariable Long id) {
        crmService.billWorkSheet(id);
        return ResponseEntity.ok().build();
    }
}

package mz.multicore.erp.modules.inventory.controller;

import mz.multicore.erp.modules.inventory.dto.*;
import mz.multicore.erp.modules.inventory.service.InventoryPhysicalCountingService;
import mz.multicore.erp.modules.printing.InventoryPhysicalCountingPrintService;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/physical-counting")
public class InventoryPhysicalCountingController {

    private final InventoryPhysicalCountingService service;
    private final InventoryPhysicalCountingPrintService printService;

    public InventoryPhysicalCountingController(
            InventoryPhysicalCountingService service,
            InventoryPhysicalCountingPrintService printService
    ) {
        this.service = service;
        this.printService = printService;
    }

    @PostMapping
    public ResponseEntity<InventorySessionDTO> createSession(@RequestBody @Valid CreateInventorySessionRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.createSession(request, companyId));
    }

    @GetMapping
    public ResponseEntity<List<InventorySessionDTO>> listSessions() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.listSessions(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventorySessionDTO> getSessionById(@PathVariable("id") Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.getSessionById(id, companyId));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<InventorySessionDTO> startCounting(@PathVariable("id") Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.startCounting(id, companyId));
    }

    @PostMapping("/{id}/count")
    public ResponseEntity<InventorySessionDTO> recordCount(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateInventoryItemCountRequest request
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.recordCount(id, request, companyId));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<InventorySessionDTO> closeAndAdjustStock(@PathVariable("id") Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.closeAndAdjustStock(id, companyId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InventorySessionDTO> cancelSession(@PathVariable("id") Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(service.cancelSession(id, companyId));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> renderPdf(@PathVariable("id") Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        InventorySessionDTO session = service.getSessionById(id, companyId);
        byte[] pdf = printService.render(session, companyId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "inventario-fisico-" + session.inventoryNumber() + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}

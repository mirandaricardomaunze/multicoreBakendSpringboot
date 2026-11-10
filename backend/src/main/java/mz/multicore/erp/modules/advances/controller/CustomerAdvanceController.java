package mz.multicore.erp.modules.advances.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.advances.dto.ApplyAdvanceRequest;
import mz.multicore.erp.modules.advances.dto.CustomerAdvanceDTO;
import mz.multicore.erp.modules.advances.dto.ReceiveAdvanceRequest;
import mz.multicore.erp.modules.advances.model.CustomerAdvanceStatus;
import mz.multicore.erp.modules.advances.service.CustomerAdvanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Customer advances: list for sellers; receiving and applying issue fiscal documents, so they are for managers. */
@RestController
@RequestMapping("/api/advances")
public class CustomerAdvanceController {

    private final CustomerAdvanceService advanceService;

    public CustomerAdvanceController(CustomerAdvanceService advanceService) {
        this.advanceService = advanceService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerAdvanceDTO>> list(@RequestParam Long companyId,
                                                         @RequestParam(required = false) CustomerAdvanceStatus status,
                                                         @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(advanceService.list(companyId, status, clientId));
    }

    @PostMapping
    public ResponseEntity<CustomerAdvanceDTO> receive(@RequestBody @Valid ReceiveAdvanceRequest request) {
        return ResponseEntity.ok(advanceService.receive(request));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<CustomerAdvanceDTO> refund(@PathVariable Long id,
                                                     @RequestBody @Valid mz.multicore.erp.modules.advances.dto.RefundAdvanceRequest request) {
        return ResponseEntity.ok(advanceService.refund(id, request));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<CustomerAdvanceDTO> apply(@PathVariable Long id, @RequestBody @Valid ApplyAdvanceRequest request) {
        return ResponseEntity.ok(advanceService.apply(id, request));
    }
}

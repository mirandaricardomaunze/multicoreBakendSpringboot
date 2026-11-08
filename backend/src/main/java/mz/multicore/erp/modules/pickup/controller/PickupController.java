package mz.multicore.erp.modules.pickup.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.pickup.dto.*;
import mz.multicore.erp.modules.pickup.service.PickupService;
import mz.multicore.erp.modules.printing.MaterialPickupPrintService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Paid material awaiting pickup (SA-C07). Sellers and yard staff open accounts and issue slips; only
 * management cancels a slip (see {@code ApiRolePolicy}).
 */
@RestController
@RequestMapping("/api/pickups")
public class PickupController {

    private final PickupService pickupService;
    private final MaterialPickupPrintService printService;

    public PickupController(PickupService pickupService, MaterialPickupPrintService printService) {
        this.pickupService = pickupService;
        this.printService = printService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<PickupAccountSummaryDTO>> accounts(@RequestParam Long companyId,
                                                                   @RequestParam(required = false) String status) {
        return ResponseEntity.ok(pickupService.list(companyId, status));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<PickupAccountDTO> account(@RequestParam Long companyId, @PathVariable Long id) {
        return ResponseEntity.ok(pickupService.get(companyId, id));
    }

    @PostMapping("/accounts")
    public ResponseEntity<PickupAccountDTO> open(@RequestBody @Valid OpenPickupAccountRequest request) {
        return ResponseEntity.ok(pickupService.open(request));
    }

    @GetMapping("/eligible-invoices")
    public ResponseEntity<List<EligibleInvoiceDTO>> eligible(@RequestParam Long companyId,
                                                             @RequestParam(required = false) String search) {
        return ResponseEntity.ok(pickupService.eligibleInvoices(companyId, search));
    }

    @GetMapping("/pending-stock")
    public ResponseEntity<List<PendingPickupStockDTO>> pendingStock(@RequestParam Long companyId) {
        return ResponseEntity.ok(pickupService.pendingStock(companyId));
    }

    @PostMapping("/slips")
    public ResponseEntity<MaterialPickupDTO> issue(@RequestBody @Valid IssuePickupRequest request) {
        return ResponseEntity.ok(pickupService.issue(request));
    }

    @PostMapping("/slips/{id}/cancel")
    public ResponseEntity<MaterialPickupDTO> cancel(@PathVariable Long id, @RequestBody @Valid CancelPickupRequest request) {
        return ResponseEntity.ok(pickupService.cancel(id, request));
    }

    @GetMapping("/slips/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Long companyId, @PathVariable Long id) {
        byte[] pdf = printService.render(companyId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"levantamento-" + id + ".pdf\"")
                .contentLength(pdf.length)
                .body(pdf);
    }
}

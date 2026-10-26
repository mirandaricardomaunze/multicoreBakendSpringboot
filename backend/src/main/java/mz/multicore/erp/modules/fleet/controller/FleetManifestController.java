package mz.multicore.erp.modules.fleet.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.service.FleetManifestService;
import mz.multicore.erp.modules.printing.FleetManifestPrintService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fleet/manifests")
public class FleetManifestController {

    private final FleetManifestService manifestService;
    private final FleetManifestPrintService printService;

    public FleetManifestController(FleetManifestService manifestService,
                                   FleetManifestPrintService printService) {
        this.manifestService = manifestService;
        this.printService = printService;
    }

    @PostMapping
    public ResponseEntity<FleetManifestDTO> createManifest(@RequestBody @Valid CreateFleetManifestRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(manifestService.createManifest(companyId, request));
    }

    @GetMapping
    public ResponseEntity<List<FleetManifestDTO>> listManifests() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(manifestService.listManifests(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FleetManifestDTO> getManifest(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(manifestService.getManifest(companyId, id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FleetManifestDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(manifestService.updateStatus(companyId, id, status));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> printManifest(@PathVariable Long id) {
        byte[] pdf = printService.renderA4(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"manifesto-carga-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

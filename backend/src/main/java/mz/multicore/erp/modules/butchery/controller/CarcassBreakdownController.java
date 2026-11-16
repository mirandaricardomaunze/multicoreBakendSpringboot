package mz.multicore.erp.modules.butchery.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.butchery.dto.*;
import mz.multicore.erp.modules.butchery.service.CarcassBreakdownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Butchery: breakdown models (managers) and carcass breakdowns (the bench posts, managers cancel). */
@RestController
@RequestMapping("/api/butchery")
public class CarcassBreakdownController {

    private final CarcassBreakdownService service;

    public CarcassBreakdownController(CarcassBreakdownService service) {
        this.service = service;
    }

    @GetMapping("/templates")
    public ResponseEntity<List<BreakdownTemplateDTO>> templates(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.templates(companyId));
    }

    @PutMapping("/templates")
    public ResponseEntity<BreakdownTemplateDTO> saveTemplate(@RequestBody @Valid SaveBreakdownTemplateRequest request) {
        return ResponseEntity.ok(service.saveTemplate(request));
    }

    @GetMapping("/breakdowns")
    public ResponseEntity<List<CarcassBreakdownDTO>> breakdowns(@RequestParam Long companyId,
                                                                @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(service.breakdowns(companyId, days));
    }

    @PostMapping("/breakdowns")
    public ResponseEntity<CarcassBreakdownDTO> post(@RequestBody @Valid PostBreakdownRequest request) {
        return ResponseEntity.ok(service.post(request));
    }

    @PostMapping("/breakdowns/{id}/cancel")
    public ResponseEntity<CarcassBreakdownDTO> cancel(@PathVariable Long id, @RequestBody @Valid CancelBreakdownRequest request) {
        return ResponseEntity.ok(service.cancel(id, request));
    }
}

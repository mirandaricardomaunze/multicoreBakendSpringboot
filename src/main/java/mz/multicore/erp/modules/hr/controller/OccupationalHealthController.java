package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.service.OccupationalHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/occupational-health")
public class OccupationalHealthController {
    private final OccupationalHealthService service;

    public OccupationalHealthController(OccupationalHealthService service) { this.service = service; }

    @GetMapping("/employee/{employeeId}/summary")
    public ResponseEntity<OccupationalHealthSummaryDTO> summary(@PathVariable Long employeeId) {
        return ResponseEntity.ok(service.summary(employeeId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<OccupationalHealthExamDTO>> history(@PathVariable Long employeeId) {
        return ResponseEntity.ok(service.history(employeeId));
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<OccupationalHealthExamDTO>> expiring() {
        return ResponseEntity.ok(service.expiring());
    }

    @PostMapping
    public ResponseEntity<OccupationalHealthExamDTO> register(
            @RequestBody @Valid SaveOccupationalHealthExamRequest request) {
        return ResponseEntity.ok(service.register(request));
    }
}

package mz.multicore.erp.modules.hr.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.service.OccupationalHealthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    /** Trabalhadores no activo sem exame de aptidão nenhum. */
    @GetMapping("/missing")
    public ResponseEntity<List<MissingHealthExamDTO>> missing() {
        return ResponseEntity.ok(service.missingExams());
    }

    /** Prestadores activos do cadastro, filtrados para o ecrã de saúde ocupacional. */
    @GetMapping("/providers")
    public ResponseEntity<List<HealthProviderDTO>> providers() {
        return ResponseEntity.ok(service.providers());
    }

    @GetMapping("/costs")
    public ResponseEntity<OccupationalHealthCostDTO> costs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.costReport(from, to));
    }

    @PostMapping
    public ResponseEntity<OccupationalHealthExamDTO> register(
            @RequestBody @Valid SaveOccupationalHealthExamRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OccupationalHealthExamDTO> pay(@PathVariable Long id) {
        return ResponseEntity.ok(service.payExam(id));
    }
}

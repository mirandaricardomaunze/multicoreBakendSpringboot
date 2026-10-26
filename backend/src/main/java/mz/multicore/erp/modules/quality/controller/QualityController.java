package mz.multicore.erp.modules.quality.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.quality.dto.*;
import mz.multicore.erp.modules.quality.service.QualityEquipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/quality")
@Validated
public class QualityController {

    private final QualityEquipmentService qualityEquipmentService;
    private final mz.multicore.erp.modules.printing.QualityEquipmentPrintService qualityEquipmentPrintService;

    public QualityController(
            QualityEquipmentService qualityEquipmentService,
            mz.multicore.erp.modules.printing.QualityEquipmentPrintService qualityEquipmentPrintService
    ) {
        this.qualityEquipmentService = qualityEquipmentService;
        this.qualityEquipmentPrintService = qualityEquipmentPrintService;
    }

    // ─── Equipamentos ────────────────────────────────────────────────────────

    @GetMapping("/equipments")
    public ResponseEntity<PageResponse<EquipmentDTO>> searchEquipments(
            @RequestParam @Positive Long companyId,
            @RequestParam(required = false) @Size(max = 160) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int size
    ) {
        return ResponseEntity.ok(qualityEquipmentService.searchEquipments(companyId, search, page, size));
    }

    @GetMapping("/equipments/active")
    public ResponseEntity<List<EquipmentDTO>> listActiveEquipments(@RequestParam @Positive Long companyId) {
        return ResponseEntity.ok(qualityEquipmentService.listActiveEquipments(companyId));
    }

    @GetMapping("/equipments/{id}")
    public ResponseEntity<EquipmentDTO> getEquipment(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long companyId
    ) {
        return ResponseEntity.ok(qualityEquipmentService.getEquipment(id, companyId));
    }

    @PostMapping("/equipments")
    public ResponseEntity<EquipmentDTO> createEquipment(@RequestBody @Valid EquipmentCreateRequest req) {
        return ResponseEntity.ok(qualityEquipmentService.createEquipment(req));
    }

    @PutMapping("/equipments/{id}")
    public ResponseEntity<EquipmentDTO> updateEquipment(
            @PathVariable @Positive Long id,
            @RequestBody @Valid EquipmentCreateRequest req
    ) {
        return ResponseEntity.ok(qualityEquipmentService.updateEquipment(id, req));
    }

    // ─── Atribuições a Trabalhadores ─────────────────────────────────────────

    @GetMapping("/assignments")
    public ResponseEntity<PageResponse<WorkerEquipmentAssignmentDTO>> searchAssignments(
            @RequestParam @Positive Long companyId,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) @Positive Long employeeId,
            @RequestParam(required = false) @Size(max = 160) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(200) int size
    ) {
        return ResponseEntity.ok(qualityEquipmentService.searchAssignments(
                companyId, status, employeeId, search, page, size
        ));
    }

    @PostMapping("/assignments")
    public ResponseEntity<WorkerEquipmentAssignmentDTO> assignEquipment(
            @RequestBody @Valid EquipmentAssignmentRequest req
    ) {
        return ResponseEntity.ok(qualityEquipmentService.assignEquipment(req));
    }

    @PostMapping("/assignments/{id}/renew")
    public ResponseEntity<WorkerEquipmentAssignmentDTO> renewAssignment(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long companyId,
            @RequestParam(required = false) LocalDate newAssignedDate,
            @RequestParam(required = false) @Size(max = 1000) String notes
    ) {
        return ResponseEntity.ok(qualityEquipmentService.renewAssignment(id, companyId, newAssignedDate, notes));
    }

    @PostMapping("/assignments/{id}/return")
    public ResponseEntity<Void> returnAssignment(
            @PathVariable @Positive Long id,
            @RequestParam @Positive Long companyId,
            @RequestParam(required = false) @Size(max = 1000) String notes
    ) {
        qualityEquipmentService.returnAssignment(id, companyId, notes);
        return ResponseEntity.ok().build();
    }

    // ─── Radar de Colaboradores e KPIs ───────────────────────────────────────

    @GetMapping("/expired-workers")
    public ResponseEntity<List<ExpiredWorkerAlertDTO>> getExpiredWorkerAlerts(@RequestParam @Positive Long companyId) {
        return ResponseEntity.ok(qualityEquipmentService.getExpiredWorkerAlerts(companyId));
    }

    @GetMapping("/kpis")
    public ResponseEntity<QualityKpiDTO> getQualityKpis(@RequestParam @Positive Long companyId) {
        return ResponseEntity.ok(qualityEquipmentService.getQualityKpis(companyId));
    }

    @GetMapping("/assignments/{id}/delivery-sheet")
    public ResponseEntity<byte[]> getDeliverySheetPdf(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) @Positive Long companyId
    ) {
        Long tenantId = CurrentUserContext.requireCurrentCompanyId();
        if (companyId != null && !tenantId.equals(companyId)) {
            throw new BusinessRuleException("Operação recusada: os dados pertencem a outra empresa.");
        }
        byte[] pdf = qualityEquipmentPrintService.renderDeliverySheet(id, tenantId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"termo-entrega-" + id + ".pdf\"")
                .body(pdf);
    }
}

package mz.multicore.erp.modules.productionplan.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.productionplan.dto.*;
import mz.multicore.erp.modules.productionplan.service.ProductionPlanService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Daily production plan of the bakery: plan, suggestion, requirements, batches made and closing of the day. */
@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController {

    private final ProductionPlanService service;

    public ProductionPlanController(ProductionPlanService service) {
        this.service = service;
    }

    @GetMapping("/day")
    public ResponseEntity<ProductionPlanDTO> day(@RequestParam Long companyId, @RequestParam Long warehouseId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.day(companyId, warehouseId, date));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<ProductionSuggestionDTO>> suggestions(@RequestParam Long companyId,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.suggestions(companyId, date));
    }

    @PutMapping
    public ResponseEntity<ProductionPlanDTO> save(@RequestBody @Valid SaveProductionPlanRequest request) {
        return ResponseEntity.ok(service.save(request));
    }

    @GetMapping("/{id}/requirements")
    public ResponseEntity<List<MaterialRequirementDTO>> requirements(@PathVariable Long id, @RequestParam Long companyId) {
        return ResponseEntity.ok(service.requirements(companyId, id));
    }

    @PostMapping("/runs")
    public ResponseEntity<ProductionRunDTO> recordRun(@RequestBody @Valid RecordRunRequest request) {
        return ResponseEntity.ok(service.recordRun(request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ProductionPlanDTO> close(@PathVariable Long id, @RequestBody @Valid ClosePlanRequest request) {
        return ResponseEntity.ok(service.close(id, request));
    }
}

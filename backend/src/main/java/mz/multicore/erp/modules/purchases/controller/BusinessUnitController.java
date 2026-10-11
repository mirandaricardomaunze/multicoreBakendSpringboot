package mz.multicore.erp.modules.purchases.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.service.BusinessUnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/business-units")
public class BusinessUnitController {

    private final BusinessUnitService businessUnitService;

    public BusinessUnitController(BusinessUnitService businessUnitService) {
        this.businessUnitService = businessUnitService;
    }

    @GetMapping
    public ResponseEntity<List<BusinessUnitDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(businessUnitService.findByCompany(companyId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BusinessUnitDTO>> listActive(@RequestParam Long companyId) {
        return ResponseEntity.ok(businessUnitService.findActiveByCompany(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessUnitDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(businessUnitService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BusinessUnitDTO> create(@RequestBody @Valid CreateBusinessUnitRequest request) {
        return ResponseEntity.ok(businessUnitService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessUnitDTO> update(@PathVariable Long id, @RequestBody @Valid UpdateBusinessUnitRequest request) {
        return ResponseEntity.ok(businessUnitService.update(id, request));
    }

    @PostMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        businessUnitService.toggleActive(id);
        return ResponseEntity.ok().build();
    }
}

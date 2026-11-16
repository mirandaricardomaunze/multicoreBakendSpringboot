package mz.multicore.erp.modules.variants.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.variants.dto.*;
import mz.multicore.erp.modules.variants.service.VariantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Variant families: everyone reads the grid; managers create families, add values and set the price. */
@RestController
@RequestMapping("/api/variants")
public class VariantController {

    private final VariantService service;

    public VariantController(VariantService service) {
        this.service = service;
    }

    @GetMapping("/families")
    public ResponseEntity<List<VariantFamilyDTO>> families(@RequestParam Long companyId) {
        return ResponseEntity.ok(service.families(companyId));
    }

    @PostMapping("/families")
    public ResponseEntity<VariantFamilyDTO> create(@RequestBody @Valid CreateVariantFamilyRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/families/{id}/values")
    public ResponseEntity<VariantFamilyDTO> addValues(@PathVariable Long id, @RequestBody @Valid AddVariantValuesRequest request) {
        return ResponseEntity.ok(service.addValues(id, request));
    }

    @PutMapping("/families/{id}/price")
    public ResponseEntity<VariantFamilyDTO> setPrice(@PathVariable Long id, @RequestBody @Valid SetFamilyPriceRequest request) {
        return ResponseEntity.ok(service.setPrice(id, request));
    }
}

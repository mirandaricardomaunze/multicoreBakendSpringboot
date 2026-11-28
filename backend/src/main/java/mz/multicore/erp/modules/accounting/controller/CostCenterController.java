package mz.multicore.erp.modules.accounting.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.accounting.dto.CostCenterDTO;
import mz.multicore.erp.modules.accounting.dto.SaveCostCenterRequest;
import mz.multicore.erp.modules.accounting.service.CostCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Centros de custo da empresa activa (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-09). Só HTTP: as guardas de
 * perfil (ADMIN/MANAGER para gerir) e de empresa vivem no {@link CostCenterService}.
 */
@RestController
@RequestMapping("/api/accounting/cost-centers")
public class CostCenterController {

    private final CostCenterService costCenterService;

    public CostCenterController(CostCenterService costCenterService) {
        this.costCenterService = costCenterService;
    }

    @GetMapping
    public ResponseEntity<List<CostCenterDTO>> list() {
        return ResponseEntity.ok(costCenterService.list());
    }

    @PostMapping
    public ResponseEntity<CostCenterDTO> create(@RequestBody @Valid SaveCostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CostCenterDTO> update(@PathVariable Long id, @RequestBody @Valid SaveCostCenterRequest request) {
        return ResponseEntity.ok(costCenterService.update(id, request));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<CostCenterDTO> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(costCenterService.setDefault(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        costCenterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

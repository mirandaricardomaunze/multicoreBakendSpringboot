package mz.multicore.erp.modules.assets.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import mz.multicore.erp.modules.assets.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ResponseEntity<List<AssetDTO>> list() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(assetService.listAssets(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDTO> getById(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(assetService.getAsset(companyId, id));
    }

    @PostMapping
    public ResponseEntity<AssetDTO> create(@RequestBody @Valid CreateAssetRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(assetService.createAsset(companyId, request));
    }

    @PostMapping("/depreciate")
    public ResponseEntity<List<DepreciationScheduleDTO>> runDepreciation(
            @RequestParam int year,
            @RequestParam int month) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(assetService.runMonthlyDepreciation(companyId, year, month));
    }

    @GetMapping("/{id}/depreciations")
    public ResponseEntity<List<DepreciationScheduleDTO>> getDepreciationHistory(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(assetService.getDepreciationHistory(companyId, id));
    }
}

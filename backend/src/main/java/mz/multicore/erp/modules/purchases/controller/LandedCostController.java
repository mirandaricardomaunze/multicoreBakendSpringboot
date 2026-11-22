package mz.multicore.erp.modules.purchases.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.purchases.dto.CreateLandedCostRequest;
import mz.multicore.erp.modules.purchases.dto.LandedCostDocumentDTO;
import mz.multicore.erp.modules.purchases.dto.LandedCostPreviewDTO;
import mz.multicore.erp.modules.purchases.service.LandedCostDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custos de Importação (série CI, docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4): pré-visualização por artigo sem
 * gravar e confirmação, sempre na empresa activa e com os perfis que registam compras.
 */
@RestController
@RequestMapping("/api/purchases/landed-costs")
public class LandedCostController {

    private final LandedCostDocumentService landedCostService;

    public LandedCostController(LandedCostDocumentService landedCostService) {
        this.landedCostService = landedCostService;
    }

    @PostMapping("/preview")
    public ResponseEntity<LandedCostPreviewDTO> preview(@RequestBody @Valid CreateLandedCostRequest request) {
        return ResponseEntity.ok(landedCostService.preview(request));
    }

    @PostMapping
    public ResponseEntity<LandedCostDocumentDTO> confirm(@RequestBody @Valid CreateLandedCostRequest request) {
        return ResponseEntity.ok(landedCostService.register(request));
    }
}

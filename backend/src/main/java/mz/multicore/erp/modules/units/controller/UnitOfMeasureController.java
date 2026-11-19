package mz.multicore.erp.modules.units.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.units.dto.*;
import mz.multicore.erp.modules.units.service.ProductUnitBarcodeService;
import mz.multicore.erp.modules.units.service.ProductUnitService;
import mz.multicore.erp.modules.units.service.UnitOfMeasureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Units of measure and product units (SA-C02). Sellers may read; only management may change them. */
@RestController
@RequestMapping("/api/units")
public class UnitOfMeasureController {

    private final UnitOfMeasureService unitService;
    private final ProductUnitService productUnitService;
    private final ProductUnitBarcodeService barcodeService;

    public UnitOfMeasureController(UnitOfMeasureService unitService, ProductUnitService productUnitService,
                                   ProductUnitBarcodeService barcodeService) {
        this.unitService = unitService;
        this.productUnitService = productUnitService;
        this.barcodeService = barcodeService;
    }

    /** POS: the barcode of a sellable alternative unit (a pallet) and how many base units it sells. Empty when unknown. */
    @GetMapping("/barcode")
    public ResponseEntity<UnitBarcodeItemDTO> findByBarcode(@RequestParam Long companyId, @RequestParam String barcode,
                                                            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(barcodeService.findSellable(companyId, barcode, warehouseId));
    }

    @GetMapping
    public ResponseEntity<List<UnitOfMeasureDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(unitService.listVisible(companyId));
    }

    @PostMapping
    public ResponseEntity<UnitOfMeasureDTO> createPackage(@RequestBody @Valid CreateUnitOfMeasureRequest request) {
        return ResponseEntity.ok(unitService.createCompanyPackage(request));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductUnitsDTO> productUnits(@RequestParam Long companyId, @PathVariable Long productId) {
        return ResponseEntity.ok(productUnitService.getProductUnits(companyId, productId));
    }

    @PutMapping("/products/base")
    public ResponseEntity<ProductUnitsDTO> setBaseUnit(@RequestBody @Valid SetBaseUnitRequest request) {
        return ResponseEntity.ok(productUnitService.setBaseUnit(request));
    }

    @PutMapping("/products")
    public ResponseEntity<ProductUnitsDTO> saveProductUnit(@RequestBody @Valid SaveProductUnitRequest request) {
        return ResponseEntity.ok(productUnitService.saveUnit(request));
    }

    @DeleteMapping("/products/units/{productUnitId}")
    public ResponseEntity<ProductUnitsDTO> deleteProductUnit(@RequestParam Long companyId,
                                                             @PathVariable Long productUnitId) {
        return ResponseEntity.ok(productUnitService.deleteUnit(companyId, productUnitId));
    }

    @GetMapping("/products/{productId}/convert")
    public ResponseEntity<UnitConversionDTO> convert(@RequestParam Long companyId, @PathVariable Long productId,
                                                     @RequestParam String unitCode, @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(productUnitService.convert(companyId, productId, unitCode, quantity));
    }
}

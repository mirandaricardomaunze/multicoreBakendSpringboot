package mz.multicore.erp.modules.manufacturing.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.manufacturing.dto.*;
import mz.multicore.erp.modules.manufacturing.service.ProductionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/manufacturing")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/boms")
    public ResponseEntity<List<BillOfMaterialsDTO>> listBoms() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.listBoms(companyId));
    }

    @GetMapping("/boms/{id}")
    public ResponseEntity<BillOfMaterialsDTO> getBom(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.getBom(companyId, id));
    }

    @PostMapping("/boms")
    public ResponseEntity<BillOfMaterialsDTO> createBom(@RequestBody @Valid CreateBomRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.createBom(companyId, request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ProductionOrderDTO>> listOrders() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.listProductionOrders(companyId));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ProductionOrderDTO> getOrder(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.getProductionOrder(companyId, id));
    }

    @PostMapping("/orders")
    public ResponseEntity<ProductionOrderDTO> createOrder(@RequestBody @Valid CreateProductionOrderRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.createProductionOrder(companyId, request));
    }

    @PostMapping("/orders/{id}/start")
    public ResponseEntity<ProductionOrderDTO> startOrder(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.startProduction(companyId, id));
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<ProductionOrderDTO> completeOrder(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal producedQuantity) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.completeProduction(companyId, id, producedQuantity));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ProductionOrderDTO> cancelOrder(@PathVariable Long id) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return ResponseEntity.ok(productionService.cancelProduction(companyId, id));
    }
}

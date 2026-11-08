package mz.multicore.erp.modules.pricing.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.pricing.dto.*;
import mz.multicore.erp.modules.pricing.service.PriceListService;
import mz.multicore.erp.modules.pricing.service.PriceResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Price lists (SA-C03). Sellers may read; only management may change them (see {@code ApiRolePolicy}). */
@RestController
@RequestMapping("/api/pricing")
public class PriceListController {

    private final PriceListService priceListService;
    private final PriceResolutionService resolutionService;

    public PriceListController(PriceListService priceListService, PriceResolutionService resolutionService) {
        this.priceListService = priceListService;
        this.resolutionService = resolutionService;
    }

    @GetMapping("/lists")
    public ResponseEntity<List<PriceListDTO>> lists(@RequestParam Long companyId) {
        return ResponseEntity.ok(priceListService.list(companyId));
    }

    @PutMapping("/lists")
    public ResponseEntity<PriceListDTO> save(@RequestBody @Valid SavePriceListRequest request) {
        return ResponseEntity.ok(priceListService.save(request));
    }

    @DeleteMapping("/lists/{id}")
    public ResponseEntity<Void> delete(@RequestParam Long companyId, @PathVariable Long id) {
        priceListService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lists/{id}/items")
    public ResponseEntity<List<PriceListItemDTO>> items(@RequestParam Long companyId, @PathVariable Long id) {
        return ResponseEntity.ok(priceListService.items(companyId, id));
    }

    @PutMapping("/items")
    public ResponseEntity<PriceListItemDTO> saveItem(@RequestBody @Valid SavePriceListItemRequest request) {
        return ResponseEntity.ok(priceListService.saveItem(request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@RequestParam Long companyId, @PathVariable Long itemId) {
        priceListService.deleteItem(companyId, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lists/{id}/clients")
    public ResponseEntity<List<ClientPriceListDTO>> clients(@RequestParam Long companyId, @PathVariable Long id) {
        return ResponseEntity.ok(priceListService.clients(companyId, id));
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<ClientPriceListDTO> clientAssignment(@RequestParam Long companyId, @PathVariable Long clientId) {
        return ResponseEntity.ok(priceListService.clientAssignment(companyId, clientId));
    }

    @PutMapping("/clients")
    public ResponseEntity<ClientPriceListDTO> assign(@RequestBody @Valid AssignClientPriceListRequest request) {
        return ResponseEntity.ok(priceListService.assign(request));
    }

    @GetMapping("/resolve")
    public ResponseEntity<ResolvedPriceDTO> resolve(@RequestParam Long companyId,
                                                    @RequestParam(required = false) Long clientId,
                                                    @RequestParam Long productId,
                                                    @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(resolutionService.resolveForApi(companyId, clientId, productId, quantity));
    }
}

package mz.multicore.erp.modules.promotions.controller;

import mz.multicore.erp.modules.promotions.dto.AppliedPromotionDTO;
import mz.multicore.erp.modules.promotions.dto.CreatePromotionRequest;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> list(@RequestParam Long companyId) {
        return ResponseEntity.ok(promotionService.findByCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO> create(@RequestBody @Valid CreatePromotionRequest request) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    @PostMapping("/{id}/active")
    public ResponseEntity<PromotionDTO> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(promotionService.setActive(id, active));
    }

    /** Melhor promoção aplicável a uma linha (produto/categoria/quantidade). Corpo {@code null} se nenhuma. */
    @GetMapping("/best")
    public ResponseEntity<AppliedPromotionDTO> best(
            @RequestParam Long companyId,
            @RequestParam Long productId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam BigDecimal quantity) {
        return ResponseEntity.ok(promotionService.bestPromotion(companyId, productId, categoryId, quantity)
                .map(p -> new AppliedPromotionDTO(p.name(), p.discountPercent()))
                .orElse(null));
    }
}

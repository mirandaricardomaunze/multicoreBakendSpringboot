package com.phcpro.modules.promotions.controller;

import com.phcpro.modules.promotions.dto.CreatePromotionRequest;
import com.phcpro.modules.promotions.dto.PromotionDTO;
import com.phcpro.modules.promotions.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

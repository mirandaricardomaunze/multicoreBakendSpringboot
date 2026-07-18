package com.phcpro.modules.promotions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PromotionDTO(
        Long id,
        String name,
        String type,
        Long productId,
        String productName,
        Long categoryId,
        String categoryName,
        BigDecimal percentValue,
        Integer buyQuantity,
        Integer payQuantity,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {}

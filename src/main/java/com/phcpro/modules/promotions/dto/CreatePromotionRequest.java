package com.phcpro.modules.promotions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedido de criação de promoção. As regras dependentes do tipo (percentagem vs leve-X-pague-Y,
 * alcance produto vs categoria) são validadas no PromotionService com {@code BusinessRuleException}.
 */
public record CreatePromotionRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotBlank(message = "Nome da promoção é obrigatório.") String name,
        @NotBlank(message = "Tipo de promoção é obrigatório.") String type,
        Long productId,
        Long categoryId,
        BigDecimal percentValue,
        Integer buyQuantity,
        Integer payQuantity,
        @NotNull(message = "Data de início é obrigatória.") LocalDate startDate,
        @NotNull(message = "Data de fim é obrigatória.") LocalDate endDate
) {}

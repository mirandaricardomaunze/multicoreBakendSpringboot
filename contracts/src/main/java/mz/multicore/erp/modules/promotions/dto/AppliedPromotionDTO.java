package mz.multicore.erp.modules.promotions.dto;

import java.math.BigDecimal;

/** Promoção aplicável a uma linha (o melhor desconto encontrado): nome + percentagem. */
public record AppliedPromotionDTO(String name, BigDecimal discountPercent) {}

package mz.multicore.erp.modules.expiry.dto;

import java.math.BigDecimal;

/** From {@code daysBefore} days before expiry, sell with {@code percent} off. No category = every perishable. */
public record MarkdownRuleDTO(Long id, Long categoryId, String categoryName, int daysBefore, BigDecimal percent, boolean active) {}

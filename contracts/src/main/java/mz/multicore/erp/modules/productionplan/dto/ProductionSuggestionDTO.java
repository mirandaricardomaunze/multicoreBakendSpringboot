package mz.multicore.erp.modules.productionplan.dto;

import java.math.BigDecimal;

/** Suggested quantity for a recipe: average sold on the same weekday over the last weeks. */
public record ProductionSuggestionDTO(Long bomId, Long productId, String productName, BigDecimal suggestedQty, int weeks) {}

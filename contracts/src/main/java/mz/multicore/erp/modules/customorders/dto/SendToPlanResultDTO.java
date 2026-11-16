package mz.multicore.erp.modules.customorders.dto;

import java.util.List;

/** Products added to the production plan of the delivery day, and those without a recipe. */
public record SendToPlanResultDTO(List<String> planned, List<String> withoutRecipe) {}

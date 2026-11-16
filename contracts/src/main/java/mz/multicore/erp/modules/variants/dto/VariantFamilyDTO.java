package mz.multicore.erp.modules.variants.dto;

import java.math.BigDecimal;
import java.util.List;

/** A family of variants (for example "Camisa Polo": Tamanho x Cor) with the totals the screen shows. */
public record VariantFamilyDTO(Long id, String name, String skuPrefix, String attribute1, String attribute2,
                               Long templateProductId, String templateProductName, boolean active,
                               BigDecimal totalStock, List<VariantDTO> variants) {}

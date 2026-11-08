package mz.multicore.erp.modules.butchery.dto;

import java.math.BigDecimal;
import java.util.List;

/** A breakdown model: which carcass it applies to and the cuts it should give. */
public record BreakdownTemplateDTO(Long id, String name, Long sourceProductId, String sourceProductName,
                                   BigDecimal lossTolerancePct, boolean active, BigDecimal expectedYieldTotal,
                                   List<BreakdownTemplateLineDTO> lines) {}

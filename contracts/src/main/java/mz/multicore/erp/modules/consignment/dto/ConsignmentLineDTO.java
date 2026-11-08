package mz.multicore.erp.modules.consignment.dto;

import java.math.BigDecimal;

/** Delivered, returned in good condition, returned damaged and, by difference, sold. */
public record ConsignmentLineDTO(Long id, Long productId, String productName, BigDecimal delivered, BigDecimal returned,
                                 BigDecimal damaged, BigDecimal sold) {}

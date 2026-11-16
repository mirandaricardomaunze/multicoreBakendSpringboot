package mz.multicore.erp.modules.customorders.dto;

import java.math.BigDecimal;

public record CustomOrderLineDTO(Long productId, String productName, BigDecimal quantity) {}

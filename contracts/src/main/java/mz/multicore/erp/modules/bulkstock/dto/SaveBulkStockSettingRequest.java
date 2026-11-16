package mz.multicore.erp.modules.bulkstock.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Marks a product as bulk stock; a {@code null} tolerance removes it from bulk measurement. */
public record SaveBulkStockSettingRequest(
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        @NotNull(message = "Produto é obrigatório.") Long productId,
        BigDecimal tolerancePercent
) {}

package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.modules.purchases.model.LandedCostType;

import java.math.BigDecimal;

/**
 * Linha de custo de um documento CI: valores na moeda da linha (valor sem IVA, IVA, total devido), a parte que entra
 * no custo e o IVA dedutível em moeda base, e a dívida criada ao fornecedor (§4.1, §4.2).
 */
public record LandedCostChargeDTO(
        LandedCostType type,
        Long supplierId,
        String supplierName,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal amount,
        BigDecimal vatRatePercent,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        BigDecimal baseCostAmount,
        BigDecimal baseVatAmount,
        Long payableId,
        String payableNumber
) {}

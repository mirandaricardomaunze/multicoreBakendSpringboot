package mz.multicore.erp.modules.purchases.dto;

import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pré-visualização de um documento CI antes de gravar (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.3), em moeda
 * base: custo que entra nos artigos, IVA dedutível, total das dívidas e, por linha de compra, o custo unitário
 * original ({@code unitCostBefore}), o custo acrescentado ({@code shareAmount}) e o custo unitário final
 * ({@code unitCostAfter}). Nada fica gravado.
 */
public record LandedCostPreviewDTO(
        LandedCostAllocationMethod allocationMethod,
        BigDecimal capitalizableCost,
        BigDecimal deductibleVat,
        BigDecimal payableTotal,
        List<LandedCostAllocationDTO> lines
) {}

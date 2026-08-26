package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;

/** Custo agregado por prestador. {@code providerId} nulo = clínicas só em texto livre. */
public record OccupationalHealthProviderCostDTO(
        Long providerId, String providerName, long examCount, BigDecimal total, BigDecimal pending
) {}

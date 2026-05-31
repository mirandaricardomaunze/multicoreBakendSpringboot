package com.phcpro.modules.fiscal.dto;

import java.math.BigDecimal;

public record TaxRateDTO(
        Long id,
        String code,
        String name,
        String type,
        BigDecimal rate,
        String legalBasis,
        boolean active
) {}

package com.phcpro.modules.comercial.dto;

import java.math.BigDecimal;

public record DebitNoteLineDTO(
        Long id,
        String description,
        BigDecimal amount,
        BigDecimal taxRate,
        BigDecimal lineTotal
) {}

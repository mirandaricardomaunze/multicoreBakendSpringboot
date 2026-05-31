package com.phcpro.modules.fiscal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WithholdingRecordDTO(
        Long id,
        LocalDate recordDate,
        String beneficiaryName,
        String beneficiaryTaxId,
        String serviceDescription,
        BigDecimal baseAmount,
        BigDecimal taxRate,
        String taxCategory,
        BigDecimal withheldAmount,
        BigDecimal netPaid,
        String status,
        LocalDate deliveredAt
) {}

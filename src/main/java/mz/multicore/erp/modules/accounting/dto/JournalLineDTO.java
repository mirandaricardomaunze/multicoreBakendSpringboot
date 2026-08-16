package mz.multicore.erp.modules.accounting.dto;

import java.math.BigDecimal;

public record JournalLineDTO(
        Long id,
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit,
        String description
) {}

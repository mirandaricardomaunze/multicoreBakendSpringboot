package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;

public record TreasuryAccountDTO(
    Long id,
    String name,
    String accountNumber,
    BigDecimal balance
) {}

package mz.multicore.erp.modules.financeira.dto;

import java.math.BigDecimal;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;

public record TreasuryAccountDTO(
    Long id,
    String name,
    String accountNumber,
    TreasuryAccountType accountType,
    BigDecimal balance
) {}

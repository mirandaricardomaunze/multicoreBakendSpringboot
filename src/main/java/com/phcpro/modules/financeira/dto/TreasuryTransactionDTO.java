package com.phcpro.modules.financeira.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TreasuryTransactionDTO(
    Long id,
    Long accountId,
    String accountName,
    String transactionType,
    BigDecimal amount,
    String description,
    LocalDateTime transactionDate
) {}

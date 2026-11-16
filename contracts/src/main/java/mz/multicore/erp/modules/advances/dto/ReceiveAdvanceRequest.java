package mz.multicore.erp.modules.advances.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Receive an advance: issues the advance invoice (VAT included in {@code amount}) and its receipt.
 * {@code requestKey} makes a retry return the same advance instead of charging twice.
 */
public record ReceiveAdvanceRequest(
        @NotNull @Positive Long companyId,
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long warehouseId,
        @NotNull @Positive Long treasuryAccountId,
        @NotBlank @Size(max = 40) String paymentMethod,
        @NotNull @Positive BigDecimal amount,
        @Positive Long orderId,
        @Size(max = 300) String notes,
        @NotBlank @Size(max = 64) String requestKey
) {}

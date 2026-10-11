package mz.multicore.erp.modules.recurring.dto;

import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação imutável de um contrato de facturação recorrente.
 */
public record RecurringContractDTO(
        Long id,
        Long companyId,
        String contractNumber,
        Long customerId,
        String customerName,
        String customerNuit,
        String description,
        ContractStatus status,
        BillingFrequency frequency,
        int billingDayOfMonth,
        BigDecimal fixedAmount,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextBillingDate,
        LocalDate lastBilledDate,
        boolean autoRenew
) {}

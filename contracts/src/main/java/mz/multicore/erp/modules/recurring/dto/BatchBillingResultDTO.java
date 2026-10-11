package mz.multicore.erp.modules.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado da execução do ciclo de facturação recorrente em lote.
 */
public record BatchBillingResultDTO(
        LocalDate executionDate,
        int contractsProcessed,
        int invoicesGenerated,
        BigDecimal totalBilledAmount,
        List<String> generatedInvoiceNumbers,
        List<String> errors
) {}

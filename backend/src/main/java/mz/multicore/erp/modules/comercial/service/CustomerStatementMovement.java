package mz.multicore.erp.modules.comercial.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movimento bruto da conta corrente de clientes (Factura, Recibo, Nota de Crédito ou Débito).
 */
public record CustomerStatementMovement(
        LocalDate date,
        String type,
        String documentNumber,
        String description,
        String reference,
        BigDecimal debit,
        BigDecimal credit,
        LocalDateTime timestamp,
        Long businessUnitId,
        String businessUnitName
) {}

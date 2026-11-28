package mz.multicore.erp.modules.portal.dto;

import java.math.BigDecimal;

/** Cliente com facturas vencidas: valor em atraso, número de facturas e maior atraso em dias. */
public record PortalOverdueClientDTO(Long clientId, String clientName, BigDecimal amount,
                                     int invoices, int maxDaysOverdue) {}

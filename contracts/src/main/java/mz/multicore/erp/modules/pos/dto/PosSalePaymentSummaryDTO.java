package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;

/**
 * Valor entregue pelo cliente e troco devolvido numa venda POS, somados sobre os pagamentos da venda.
 * Pagamentos que não são em numerário contam o próprio valor como entregue e não têm troco.
 */
public record PosSalePaymentSummaryDTO(
        Long invoiceId,
        BigDecimal tenderedAmount,
        BigDecimal changeGiven
) {}

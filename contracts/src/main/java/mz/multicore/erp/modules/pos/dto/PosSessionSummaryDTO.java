package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumo de sessão de caixa para auditoria e histórico de fechos de caixa (Z).
 */
public record PosSessionSummaryDTO(
        Long sessionId,
        String operator,
        LocalDateTime openDate,
        LocalDateTime closeDate,
        String status,
        BigDecimal openingBalance,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        BigDecimal totalSalesAmount,
        int saleCount
) {}

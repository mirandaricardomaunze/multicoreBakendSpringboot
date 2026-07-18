package com.phcpro.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dados do fecho de caixa (Z) — reconciliação da gaveta de uma sessão. {@code countedCash} e
 * {@code difference} vêm null enquanto a sessão está aberta (pré-visualização).
 */
public record PosZReportDTO(
        Long sessionId,
        String operator,
        LocalDateTime openDate,
        LocalDateTime closeDate,
        String status,
        BigDecimal openingBalance,
        BigDecimal cashSales,
        BigDecimal suprimentos,
        BigDecimal sangrias,
        BigDecimal refunds,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        int saleCount
) {}

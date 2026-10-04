package mz.multicore.erp.modules.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Dados do fecho de caixa (Z) — reconciliação da gaveta e desagregação por meio de pagamento.
 * {@code countedCash} e {@code difference} vêm null enquanto a sessão está aberta (pré-visualização).
 */
public record PosZReportDTO(
        Long sessionId,
        String operator,
        String currentOperator,
        LocalDateTime openDate,
        LocalDateTime closeDate,
        String status,
        BigDecimal openingBalance,
        BigDecimal cashSales,
        BigDecimal cardSales,
        BigDecimal mpesaSales,
        BigDecimal chequeSales,
        BigDecimal creditSales,
        BigDecimal totalSales,
        BigDecimal suprimentos,
        BigDecimal sangrias,
        BigDecimal refunds,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal difference,
        int saleCount,
        int refundsCount,
        String closingNotes,
        String cashBreakdownJson,
        List<ShiftReconciliationDTO> shiftReconciliations
) {
    public PosZReportDTO {
        if (shiftReconciliations == null) {
            shiftReconciliations = Collections.emptyList();
        }
    }

    /** Construtor para compatibilidade com versões que não tinham currentOperator nem shiftReconciliations. */
    public PosZReportDTO(
            Long sessionId,
            String operator,
            LocalDateTime openDate,
            LocalDateTime closeDate,
            String status,
            BigDecimal openingBalance,
            BigDecimal cashSales,
            BigDecimal cardSales,
            BigDecimal mpesaSales,
            BigDecimal chequeSales,
            BigDecimal creditSales,
            BigDecimal totalSales,
            BigDecimal suprimentos,
            BigDecimal sangrias,
            BigDecimal refunds,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference,
            int saleCount,
            int refundsCount,
            String closingNotes,
            String cashBreakdownJson
    ) {
        this(
                sessionId,
                operator,
                null,
                openDate,
                closeDate,
                status,
                openingBalance,
                cashSales,
                cardSales,
                mpesaSales,
                chequeSales,
                creditSales,
                totalSales,
                suprimentos,
                sangrias,
                refunds,
                expectedCash,
                countedCash,
                difference,
                saleCount,
                refundsCount,
                closingNotes,
                cashBreakdownJson,
                Collections.emptyList()
        );
    }

    /** Construtor para compatibilidade com versões que não tinham closingNotes e cashBreakdownJson. */
    public PosZReportDTO(
            Long sessionId,
            String operator,
            LocalDateTime openDate,
            LocalDateTime closeDate,
            String status,
            BigDecimal openingBalance,
            BigDecimal cashSales,
            BigDecimal cardSales,
            BigDecimal mpesaSales,
            BigDecimal chequeSales,
            BigDecimal creditSales,
            BigDecimal totalSales,
            BigDecimal suprimentos,
            BigDecimal sangrias,
            BigDecimal refunds,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal difference,
            int saleCount,
            int refundsCount
    ) {
        this(
                sessionId,
                operator,
                openDate,
                closeDate,
                status,
                openingBalance,
                cashSales,
                cardSales,
                mpesaSales,
                chequeSales,
                creditSales,
                totalSales,
                suprimentos,
                sangrias,
                refunds,
                expectedCash,
                countedCash,
                difference,
                saleCount,
                refundsCount,
                null,
                null
        );
    }

    /** Construtor de compatibilidade para código legado que omitia desagregações por meio de pagamento. */
    public PosZReportDTO(
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
    ) {
        this(
                sessionId,
                operator,
                openDate,
                closeDate,
                status,
                openingBalance,
                cashSales,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                cashSales != null ? cashSales : BigDecimal.ZERO,
                suprimentos,
                sangrias,
                refunds,
                expectedCash,
                countedCash,
                difference,
                saleCount,
                0,
                null,
                null
        );
    }
}

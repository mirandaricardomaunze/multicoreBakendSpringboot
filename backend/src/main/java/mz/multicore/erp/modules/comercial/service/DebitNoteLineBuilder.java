package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteLineRequest;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.model.DebitNoteLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Valida as linhas de uma nota de débito e calcula os seus totais. */
final class DebitNoteLineBuilder {

    private DebitNoteLineBuilder() {}

    static void addLines(DebitNote note, List<CreateDebitNoteLineRequest> requests) {
        BigDecimal totalBefore = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateDebitNoteLineRequest lineReq : requests) {
            BigDecimal amount = lineReq.amount();
            BigDecimal taxRate = lineReq.taxRate() == null ? BigDecimal.ZERO : lineReq.taxRate();
            if (amount.signum() <= 0 || taxRate.compareTo(BigDecimal.ONE) >= 0) {
                throw new BusinessRuleException("Indique um valor positivo e a taxa de IVA em fracção (0,16 para 16%).");
            }
            LineCalculator.LineAmounts amounts =
                    LineCalculator.compute(amount, BigDecimal.ONE, BigDecimal.ZERO, taxRate);

            DebitNoteLine line = new DebitNoteLine();
            line.setDescription(lineReq.description());
            line.setAmount(amount);
            line.setTaxRate(taxRate);
            line.setLineTotal(amounts.total());
            note.addLine(line);

            totalBefore = totalBefore.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        note.setTotalBeforeTax(totalBefore.setScale(2, RoundingMode.HALF_UP));
        note.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        note.setTotalAmount(totalBefore.add(totalTax).setScale(2, RoundingMode.HALF_UP));
    }
}

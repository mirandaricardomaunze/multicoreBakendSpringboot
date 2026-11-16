package mz.multicore.erp.modules.exceptions.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.exceptions.dto.ExceptionFindingDTO;
import mz.multicore.erp.modules.exceptions.model.ExceptionSeverity;
import mz.multicore.erp.modules.exceptions.repository.ExceptionQueryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Butchery controls: breakdowns that lost more than the model accepts, and operators whose stock wastes are far
 * above the others in the period (one person writing off much more than the rest is the classic leak).
 */
@Component
public class ButcheryExceptionChecks implements ExceptionCheck {

    static final String SECTOR = "Talho";
    /** An operator is flagged when his wastes are at least this many times the average of the operators. */
    static final BigDecimal WASTE_FACTOR = new BigDecimal("2");

    private final ExceptionQueryRepository queries;

    public ButcheryExceptionChecks(ExceptionQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<ExceptionFindingDTO> run(Long companyId, LocalDate from, LocalDate to) {
        List<ExceptionFindingDTO> findings = new ArrayList<>();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        for (var b : queries.breakdowns(companyId, start, end)) {
            if (b.getLossTolerancePct() == null || b.getInputKg().signum() == 0) continue;
            BigDecimal lossPct = b.getInputKg().subtract(b.getOutputKg()).multiply(BigDecimal.valueOf(100))
                    .divide(b.getInputKg(), 2, RoundingMode.HALF_UP);
            if (lossPct.compareTo(b.getLossTolerancePct()) <= 0) continue;
            BigDecimal over = lossPct.subtract(b.getLossTolerancePct());
            findings.add(new ExceptionFindingDTO(SECTOR, "BREAKDOWN_LOSS", over.compareTo(BigDecimal.valueOf(5)) > 0
                    ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM, "Desmancha com perda acima do aceite",
                    b.getSourceProduct().getName() + ": perda de " + DisplayNumbers.qty(lossPct) + "% (aceite "
                            + DisplayNumbers.qty(b.getLossTolerancePct()) + "%).", b.getCreatedBy(), b.getCreatedAt().toLocalDate(),
                    b.getNumber(), b.getInputKg().subtract(b.getOutputKg())));
        }
        Map<String, BigDecimal> byOperator = new HashMap<>();
        Map<String, Integer> countByOperator = new HashMap<>();
        for (var w : queries.wastes(companyId, start, end)) {
            String who = w.getRegisteredBy() == null ? "—" : w.getRegisteredBy();
            byOperator.merge(who, w.getTotalCost() == null ? BigDecimal.ZERO : w.getTotalCost(), BigDecimal::add);
            countByOperator.merge(who, 1, Integer::sum);
        }
        findings.addAll(outliers(byOperator, countByOperator, SECTOR, to));
        return findings;
    }

    /** Operators whose total is at least {@link #WASTE_FACTOR} times the average; needs two operators or more. */
    static List<ExceptionFindingDTO> outliers(Map<String, BigDecimal> totals, Map<String, Integer> counts, String sector, LocalDate date) {
        List<ExceptionFindingDTO> result = new ArrayList<>();
        if (totals.size() < 2) return result;
        BigDecimal average = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totals.size()), 2, RoundingMode.HALF_UP);
        if (average.signum() == 0) return result;
        totals.forEach((who, total) -> {
            if (total.compareTo(average.multiply(WASTE_FACTOR)) < 0) return;
            result.add(new ExceptionFindingDTO(sector, "WASTE_BY_OPERATOR", ExceptionSeverity.HIGH, "Quebras muito acima da média",
                    who + " registou " + counts.get(who) + " quebras no valor de " + DisplayNumbers.money(total) + " MT; a média dos "
                            + "operadores é " + DisplayNumbers.money(average) + " MT.", who, date, null, total));
        });
        return result;
    }
}

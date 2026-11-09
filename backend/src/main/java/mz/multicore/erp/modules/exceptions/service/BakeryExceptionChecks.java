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

/** Bakery controls: closed days with too many leftovers, and consignments left at resellers without settlement. */
@Component
public class BakeryExceptionChecks implements ExceptionCheck {

    static final String SECTOR = "Padaria";
    static final BigDecimal LEFTOVER_LIMIT_PCT = new BigDecimal("20");
    static final int CONSIGNMENT_DAYS = 2;

    private final ExceptionQueryRepository queries;

    public BakeryExceptionChecks(ExceptionQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<ExceptionFindingDTO> run(Long companyId, LocalDate from, LocalDate to) {
        List<ExceptionFindingDTO> findings = new ArrayList<>();
        for (var l : queries.closedPlanLines(companyId, from, to)) {
            if (l.getProducedQty().signum() == 0) continue;
            BigDecimal pct = l.getLeftoverQty().multiply(BigDecimal.valueOf(100)).divide(l.getProducedQty(), 1, RoundingMode.HALF_UP);
            if (pct.compareTo(LEFTOVER_LIMIT_PCT) <= 0) continue;
            findings.add(new ExceptionFindingDTO(SECTOR, "LEFTOVERS_HIGH", pct.compareTo(BigDecimal.valueOf(40)) > 0
                    ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM, "Sobras acima de " + DisplayNumbers.qty(LEFTOVER_LIMIT_PCT) + "%",
                    l.getProduct().getName() + ": sobraram " + DisplayNumbers.qty(l.getLeftoverQty()) + " de "
                            + DisplayNumbers.qty(l.getProducedQty()) + " (" + DisplayNumbers.qty(pct) + "%). Reveja o plano.",
                    l.getPlan().getClosedBy(), l.getPlan().getPlanDate(), "Plano " + l.getPlan().getPlanDate(), l.getLeftoverQty()));
        }
        LocalDate today = LocalDate.now();
        for (var d : queries.unsettledConsignments(companyId, today.minusDays(CONSIGNMENT_DAYS))) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(d.getDeliveryDate(), today);
            findings.add(new ExceptionFindingDTO(SECTOR, "CONSIGNMENT_UNSETTLED", days > 7 ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM,
                    "Consignação sem acerto", "Guia entregue a " + d.getClient().getName() + " há " + days + " dias, ainda sem acerto nem fatura.",
                    d.getCreatedBy(), d.getDeliveryDate(), d.getNumber(), null));
        }
        return findings;
    }
}

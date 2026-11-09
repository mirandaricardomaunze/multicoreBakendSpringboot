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
import mz.multicore.erp.modules.bulkstock.model.BulkMeasurementStatus;
import mz.multicore.erp.modules.returnables.service.ReturnableService;

/**
 * Building-materials yard controls: delivery guides never invoiced, overdue pickups, bulk counts out of tolerance,
 * weights typed by hand on the truck scale and returnables out for too long.
 */
@Component
public class YardExceptionChecks implements ExceptionCheck {

    static final String SECTOR = "Estaleiro";
    static final int GUIDE_DAYS = 3;
    static final int RETURNABLE_DAYS = 60;

    private final ExceptionQueryRepository queries;
    private final ReturnableService returnables;

    public YardExceptionChecks(ExceptionQueryRepository queries, ReturnableService returnables) {
        this.queries = queries;
        this.returnables = returnables;
    }

    @Override
    public List<ExceptionFindingDTO> run(Long companyId, LocalDate from, LocalDate to) {
        List<ExceptionFindingDTO> findings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (var g : queries.guidesWithoutInvoice(companyId, today.minusDays(GUIDE_DAYS).atStartOfDay())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(g.getGuideDate().toLocalDate(), today);
            findings.add(new ExceptionFindingDTO(SECTOR, "GUIDE_WITHOUT_INVOICE", days > 10 ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM,
                    "Guia sem fatura", "Material saiu há " + days + " dias com a guia e ainda não foi faturado.",
                    g.getResponsible(), g.getGuideDate().toLocalDate(), g.getGuideNumber(), g.getTotalAmount()));
        }
        for (var a : queries.overduePickups(companyId, today)) {
            String client = a.getInvoice().getClient() == null ? "cliente" : a.getInvoice().getClient().getName();
            findings.add(new ExceptionFindingDTO(SECTOR, "PICKUP_OVERDUE", ExceptionSeverity.LOW, "Levantamento com prazo vencido",
                    client + " tinha até " + a.getDeadline() + " para levantar o material da fatura " + a.getInvoice().getInvoiceNumber() + ".",
                    a.getCreatedBy(), a.getDeadline(), a.getInvoice().getInvoiceNumber(), null));
        }
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        for (var m : queries.bulkMeasurements(companyId, start, end)) {
            if (m.getStatus() == BulkMeasurementStatus.NO_DIFFERENCE || m.getDifferencePercent() == null || m.getTolerancePercent() == null) continue;
            if (m.getDifferencePercent().abs().compareTo(m.getTolerancePercent()) <= 0) continue;
            findings.add(new ExceptionFindingDTO(SECTOR, "BULK_OUT_OF_TOLERANCE", m.getStatus() == BulkMeasurementStatus.APPLIED
                    ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM, "Granel fora da tolerância",
                    m.getProduct().getName() + ": diferença de " + DisplayNumbers.qty(m.getDifferencePercent()) + "% (tolerância "
                            + DisplayNumbers.qty(m.getTolerancePercent()) + "%), estado " + m.getStatus().label().toLowerCase() + ".",
                    m.getCreatedBy(), m.getCreatedAt().toLocalDate(), "Medição " + m.getId(), m.getDifference()));
        }
        for (var t : queries.manualWeighings(companyId, start, end)) {
            findings.add(new ExceptionFindingDTO(SECTOR, "WEIGHING_MANUAL", ExceptionSeverity.MEDIUM, "Pesagem escrita à mão",
                    "Camião " + t.getPlate() + (t.getNetWeight() == null ? " ainda no recinto" : ": líquido " + DisplayNumbers.qty(t.getNetWeight()) + " kg")
                            + " com peso não lido da báscula.", t.getCreatedBy(), t.getFirstAt().toLocalDate(), t.getNumber(), t.getNetWeight()));
        }
        for (var b : returnables.balances(companyId, null)) {
            if (b.daysOut() <= RETURNABLE_DAYS) continue;
            findings.add(new ExceptionFindingDTO(SECTOR, "RETURNABLES_LONG_OUT", ExceptionSeverity.LOW, "Vasilhame fora há muito tempo",
                    b.clientName() + " tem " + DisplayNumbers.qty(b.balance()) + " " + b.itemName() + " há " + b.daysOut() + " dias.",
                    null, b.lastMovement(), b.itemName(), b.value()));
        }
        return findings;
    }
}

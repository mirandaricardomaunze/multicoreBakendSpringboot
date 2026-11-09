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
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;

/**
 * Controls for every business: operators who cancel documents often, large discounts and cash differences at the
 * close of the till.
 */
@Component
public class GeneralExceptionChecks implements ExceptionCheck {

    static final String SECTOR = "Geral";
    static final List<String> CANCEL_ACTIONS = List.of("INVOICE_CANCEL", "RECEIPT_CANCEL", "CREDIT_NOTE_CANCEL", "DEBIT_NOTE_CANCEL",
            "ORDER_CANCEL", "DELIVERY_GUIDE_CANCEL", "MATERIAL_PICKUP_CANCEL", "CARCASS_BREAKDOWN_CANCEL", "POS_RETURN", "POS_REFUND_CASH");
    static final int CANCEL_LIMIT = 3;
    static final BigDecimal DISCOUNT_LIMIT = new BigDecimal("10");
    static final BigDecimal CASH_LIMIT = new BigDecimal("100");

    private final ExceptionQueryRepository queries;

    public GeneralExceptionChecks(ExceptionQueryRepository queries) {
        this.queries = queries;
    }

    @Override
    public List<ExceptionFindingDTO> run(Long companyId, LocalDate from, LocalDate to) {
        List<ExceptionFindingDTO> findings = new ArrayList<>();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        Map<String, Integer> cancels = new TreeMap<>();
        for (var a : queries.audit(companyId, start, end, CANCEL_ACTIONS)) cancels.merge(a.getUsername() == null ? "—" : a.getUsername(), 1, Integer::sum);
        cancels.forEach((who, count) -> {
            if (count < CANCEL_LIMIT) return;
            findings.add(new ExceptionFindingDTO(SECTOR, "CANCELLATIONS_BY_OPERATOR", count >= CANCEL_LIMIT * 3 ? ExceptionSeverity.HIGH
                    : ExceptionSeverity.MEDIUM, "Muitas anulações e devoluções", who + " fez " + count
                    + " anulações ou devoluções no período. Confirme os motivos na auditoria.", who, to, null, BigDecimal.valueOf(count)));
        });
        Map<String, BigDecimal> discounts = new TreeMap<>();
        Map<String, Integer> discountCount = new HashMap<>();
        for (var l : queries.discountedLines(companyId, start, end, DISCOUNT_LIMIT, InvoiceStatus.realisedSaleStatuses())) {
            String who = l.getInvoice().getCreatedBy() == null ? "—" : l.getInvoice().getCreatedBy();
            BigDecimal gross = l.getUnitPrice().multiply(l.getQuantity());
            discounts.merge(who, gross.multiply(l.getDiscountPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP), BigDecimal::add);
            discountCount.merge(who, 1, Integer::sum);
        }
        discounts.forEach((who, value) -> findings.add(new ExceptionFindingDTO(SECTOR, "DISCOUNTS_BY_OPERATOR",
                discountCount.get(who) >= 10 ? ExceptionSeverity.HIGH : ExceptionSeverity.LOW, "Descontos acima de "
                + DisplayNumbers.qty(DISCOUNT_LIMIT) + "%", who + " deu " + discountCount.get(who) + " descontos acima de "
                + DisplayNumbers.qty(DISCOUNT_LIMIT) + "%, no total de " + DisplayNumbers.money(value) + " MT.", who, to, null, value)));
        for (var t : queries.closedTills(companyId, start, end)) {
            if (t.getDifference().abs().compareTo(CASH_LIMIT) <= 0) continue;
            boolean missing = t.getDifference().signum() < 0;
            findings.add(new ExceptionFindingDTO(SECTOR, "CASH_DIFFERENCE", missing && t.getDifference().abs().compareTo(CASH_LIMIT.multiply(BigDecimal.TEN)) > 0
                    ? ExceptionSeverity.HIGH : ExceptionSeverity.MEDIUM, missing ? "Falta de caixa no fecho" : "Sobra de caixa no fecho",
                    "Caixa de " + t.getOperator() + " fechou com " + DisplayNumbers.money(t.getDifference()) + " MT de diferença.",
                    t.getOperator(), t.getCloseDate().toLocalDate(), "Sessão " + t.getId(), t.getDifference()));
        }
        return findings;
    }
}

package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Valor entregue e troco das vendas POS, para o histórico mostrar o que foi gravado no pagamento. */
@Service
public class PosSalePaymentQueryService {

    static final int MAX_INVOICES = 200;

    private final PaymentEntryRepository paymentEntryRepository;

    public PosSalePaymentQueryService(PaymentEntryRepository paymentEntryRepository) {
        this.paymentEntryRepository = paymentEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<PosSalePaymentSummaryDTO> summarize(List<Long> invoiceIds) {
        PermissionGuard.requireSeller("consultar pagamentos de vendas POS");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Set<Long> ids = new LinkedHashSet<>();
        if (invoiceIds != null) invoiceIds.stream().filter(Objects::nonNull).forEach(ids::add);
        if (ids.isEmpty()) return List.of();
        if (ids.size() > MAX_INVOICES) {
            throw new BusinessRuleException("Consulte no máximo " + MAX_INVOICES + " vendas de cada vez.");
        }
        return sumByInvoice(paymentEntryRepository.findByInvoiceCompanyIdAndInvoiceIdIn(companyId, ids));
    }

    static List<PosSalePaymentSummaryDTO> sumByInvoice(List<PaymentEntry> entries) {
        Map<Long, BigDecimal[]> totals = new LinkedHashMap<>();
        for (PaymentEntry entry : entries) {
            BigDecimal[] sum = totals.computeIfAbsent(entry.getInvoice().getId(),
                    id -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal tendered = entry.getTenderedAmount() != null ? entry.getTenderedAmount() : entry.getAmount();
            sum[0] = sum[0].add(tendered == null ? BigDecimal.ZERO : tendered);
            sum[1] = sum[1].add(entry.getChangeGiven() == null ? BigDecimal.ZERO : entry.getChangeGiven());
        }
        return totals.entrySet().stream()
                .map(e -> new PosSalePaymentSummaryDTO(e.getKey(),
                        e.getValue()[0].setScale(2, RoundingMode.HALF_UP),
                        e.getValue()[1].setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }
}

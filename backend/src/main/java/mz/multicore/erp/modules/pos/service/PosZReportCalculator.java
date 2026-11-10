package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.SalesChannel;
import mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.ShiftReconciliationRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class PosZReportCalculator {

    private final TillSessionRepository tillSessionRepository;
    private final TillMovementRepository tillMovementRepository;
    private final PaymentEntryRepository paymentEntryRepository;
    private final ShiftReconciliationRepository shiftReconciliationRepository;
    private final PosSessionAndShiftService sessionAndShiftService;

    public PosZReportCalculator(TillSessionRepository tillSessionRepository,
                                TillMovementRepository tillMovementRepository,
                                PaymentEntryRepository paymentEntryRepository,
                                ShiftReconciliationRepository shiftReconciliationRepository,
                                PosSessionAndShiftService sessionAndShiftService) {
        this.tillSessionRepository = tillSessionRepository;
        this.tillMovementRepository = tillMovementRepository;
        this.paymentEntryRepository = paymentEntryRepository;
        this.shiftReconciliationRepository = shiftReconciliationRepository;
        this.sessionAndShiftService = sessionAndShiftService;
    }

    public PosZReportDTO buildZReport(Long sessionId) {
        TillSession session = tillSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de caixa não encontrada."));
        CurrentUserContext.requireCompany(session.getCompany().getId());

        BigDecimal opening = session.getOpeningBalance() == null ? BigDecimal.ZERO : session.getOpeningBalance();
        BigDecimal cashSales = BigDecimal.ZERO;
        BigDecimal suprimentos = BigDecimal.ZERO;
        BigDecimal sangrias = BigDecimal.ZERO;
        BigDecimal refunds = BigDecimal.ZERO;
        BigDecimal counterReceipts = BigDecimal.ZERO;
        int saleCount = 0;
        int refundsCount = 0;
        for (TillMovement m : tillMovementRepository.findByTillSessionId(sessionId)) {
            switch (m.getMovementType()) {
                case SALE -> { cashSales = cashSales.add(m.getAmount()); saleCount++; }
                case SUPRIMENTO -> suprimentos = suprimentos.add(m.getAmount());
                case SANGRIA -> sangrias = sangrias.add(m.getAmount());
                case REFUND -> { refunds = refunds.add(m.getAmount()); refundsCount++; }
                case COUNTER_RECEIPT, COUNTER_RECEIPT_REVERSAL -> counterReceipts = counterReceipts.add(m.getMovementType().signed(m.getAmount()));
            }
        }

        LocalDateTime openDate = session.getOpenDate();
        LocalDateTime closeDate = session.getCloseDate() != null ? session.getCloseDate() : LocalDateTime.now();

        BigDecimal cardSales = BigDecimal.ZERO;
        BigDecimal mpesaSales = BigDecimal.ZERO;
        BigDecimal chequeSales = BigDecimal.ZERO;
        BigDecimal creditSales = BigDecimal.ZERO;

        List<PaymentEntry> entries = paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(
                session.getCompany().getId(), openDate, closeDate);

        for (PaymentEntry entry : entries) {
            if (entry.getInvoice() != null && SalesChannel.POS == entry.getInvoice().getSalesChannel()
                    && (session.getOperator() == null || session.getOperator().equalsIgnoreCase(entry.getInvoice().getCreatedBy()))) {
                if (entry.getMethod() != null) {
                    switch (entry.getMethod()) {
                        case CARD -> cardSales = cardSales.add(entry.getAmount());
                        case MPESA, EMOLA -> mpesaSales = mpesaSales.add(entry.getAmount());
                        case BANK_TRANSFER -> chequeSales = chequeSales.add(entry.getAmount());
                        case CREDIT -> creditSales = creditSales.add(entry.getAmount());
                        default -> {}
                    }
                }
            }
        }

        BigDecimal totalSales = cashSales.add(cardSales).add(mpesaSales).add(chequeSales).add(creditSales);
        BigDecimal expected = opening.add(cashSales).add(suprimentos).subtract(sangrias).subtract(refunds).add(counterReceipts);
        BigDecimal counted = session.getClosingBalanceReal();
        BigDecimal difference = counted == null ? null : counted.subtract(expected);

        List<ShiftReconciliationDTO> recons = shiftReconciliationRepository != null
                ? shiftReconciliationRepository.findByTillSessionIdOrderByReconciledAtAsc(sessionId)
                        .stream().map(sessionAndShiftService::toDTO).toList()
                : Collections.emptyList();

        return new PosZReportDTO(
                session.getId(), session.getOperator(), session.getCurrentOperator(),
                session.getOpenDate(), session.getCloseDate(), session.getStatus(),
                opening, cashSales, cardSales, mpesaSales, chequeSales, creditSales,
                totalSales, suprimentos, sangrias, refunds, expected, counted, difference,
                saleCount, refundsCount, session.getClosingNotes(), session.getCashBreakdownJson(), recons, counterReceipts
        );
    }

    public List<PosSessionSummaryDTO> getSessionsHistory(Long companyId, LocalDateTime from, LocalDateTime to) {
        CurrentUserContext.requireCompany(companyId);
        List<TillSession> sessions = (from != null && to != null)
                ? tillSessionRepository.findByCompanyIdAndOpenDateBetweenOrderByOpenDateDesc(companyId, from, to)
                : tillSessionRepository.findByCompanyIdOrderByOpenDateDesc(companyId);
        return sessions.stream().map(s -> {
            PosZReportDTO z = buildZReport(s.getId());
            return new PosSessionSummaryDTO(
                    s.getId(), s.getOperator(), s.getOpenDate(), s.getCloseDate(), s.getStatus(),
                    s.getOpeningBalance(), z.expectedCash(), s.getClosingBalanceReal(),
                    s.getDifference(), z.totalSales(), z.saleCount()
            );
        }).toList();
    }
}

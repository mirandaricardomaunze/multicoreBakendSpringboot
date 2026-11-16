package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.events.PaymentReceivedEvent;
import mz.multicore.erp.architecture.events.ReceiptCancelledEvent;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cash received at the counter for a receipt (sinal, vasilhame, an invoice in debt) goes into the drawer of the
 * operator's open POS session, so the close count matches what is physically there. The receipt keeps its
 * treasury posting; the close deposit leaves these amounts out so they are not counted twice.
 * See {@code docs/GAVETA_DINHEIRO_SPEC.md} and {@code MOVIMENTOS_COMERCIAIS.md}.
 */
@Component
public class CounterReceiptTillListener {

    private static final String OPEN = "OPEN";

    private final TillSessionRepository tillSessionRepository;
    private final TillMovementRepository tillMovementRepository;

    public CounterReceiptTillListener(TillSessionRepository tillSessionRepository,
                                      TillMovementRepository tillMovementRepository) {
        this.tillSessionRepository = tillSessionRepository;
        this.tillMovementRepository = tillMovementRepository;
    }

    @EventListener
    public void onPaymentReceived(PaymentReceivedEvent event) {
        if (!event.cashPayment() || event.amount() == null || event.amount().signum() <= 0) return;
        String operator = CurrentUserContext.getUsername();
        if (operator == null || event.companyId() == null) return;
        tillSessionRepository.findActiveSessionForOperator(operator, OPEN, event.companyId())
                .ifPresent(session -> save(session, TillMovementType.COUNTER_RECEIPT, event.amount(),
                        description(event.receiptNumber()), operator));
    }

    @EventListener
    public void onReceiptCancelled(ReceiptCancelledEvent event) {
        tillMovementRepository.findFirstByMovementTypeAndDescriptionAndTillSessionCompanyId(
                        TillMovementType.COUNTER_RECEIPT, description(event.receiptNumber()), event.companyId())
                .filter(movement -> OPEN.equals(movement.getTillSession().getStatus()))
                .ifPresent(movement -> save(movement.getTillSession(), TillMovementType.COUNTER_RECEIPT_REVERSAL,
                        movement.getAmount(), "Anulação do recibo " + event.receiptNumber(), CurrentUserContext.getUsername()));
    }

    private static String description(String receiptNumber) {
        return "Recibo " + receiptNumber;
    }

    private void save(TillSession session, TillMovementType type, BigDecimal amount, String description, String operator) {
        TillMovement movement = new TillMovement();
        movement.setTillSession(session);
        movement.setMovementType(type);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setMovementDate(LocalDateTime.now());
        movement.setCreatedBy(operator != null ? operator : session.getOperator());
        tillMovementRepository.save(movement);
    }
}

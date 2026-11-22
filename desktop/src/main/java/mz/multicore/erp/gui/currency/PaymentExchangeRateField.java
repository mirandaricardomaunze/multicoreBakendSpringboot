package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Exchange rate of a supplier payment and the exchange difference shown before confirming
 * (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.4, §4.5). A purchase in the base currency has no rate and the
 * field stays hidden. The preview follows the backend rule: treasury = amount × payment rate; the debt goes down at
 * the document rate (difference of the base balance before/after, or exactly what was left when it settles).
 */
public final class PaymentExchangeRateField extends JPanel {

    private static final int RATE_SCALE = 4;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Amounts in the base currency; a positive difference is a loss (6601), a negative one a gain (7601). */
    public record Preview(BigDecimal treasuryAmount, BigDecimal supplierReduction, BigDecimal difference) { }

    private final PayableDTO payable;
    private final DecimalField rateField = new DecimalField("", RATE_SCALE, true);
    private final JLabel hint = new JLabel(" ");
    private final JLabel difference = new JLabel(" ");
    private BigDecimal amount = BigDecimal.ZERO;

    /** @param exchangeRateApiClient source of the rate proposed for today; null proposes nothing. */
    public PaymentExchangeRateField(PayableDTO payable, ExchangeRateApiClient exchangeRateApiClient) {
        super(new BorderLayout(8, 2));
        this.payable = payable;
        setOpaque(false);
        setVisible(isForeign());
        rateField.setToolTipText("Câmbio do pagamento: quanto vale 1 " + payable.currency() + " na moeda base hoje.");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        difference.setFont(Typography.CAPTION);
        JPanel labels = new JPanel(new GridLayout(2, 1));
        labels.setOpaque(false);
        labels.add(hint);
        labels.add(difference);
        add(rateField, BorderLayout.CENTER);
        add(labels, BorderLayout.SOUTH);
        rateField.getDocument().addDocumentListener(onChange(this::refreshDifference));
        if (isForeign() && exchangeRateApiClient != null) {
            UIHelper.loadAsync(this, () -> exchangeRateApiClient.propose(payable.currency(), LocalDate.now()),
                    this::applyProposal, error -> hint.setText("Câmbio proposto indisponível: escreva o câmbio."));
        }
    }

    /** Foreign purchase: has a currency and a document rate other than 1 (the base currency is always rate 1). */
    public boolean isForeign() {
        return payable.currency() != null && !payable.currency().isBlank()
                && payable.exchangeRate() != null && payable.exchangeRate().compareTo(BigDecimal.ONE) != 0;
    }

    public void applyProposal(ProposedExchangeRateDTO proposal) {
        if (proposal == null || !isForeign() || !payable.currency().equals(proposal.currency())) return;
        if (proposal.manualRateRequired() || proposal.rate() == null) {
            rateField.setText("");
            hint.setText("Sem câmbio na tabela: escreva o câmbio do pagamento.");
        } else {
            rateField.setText(DisplayNumbers.rate(proposal.rate()));
            hint.setText("Câmbio de " + DATE.format(proposal.rateDate()) + " (pode alterar). Documento: "
                    + DisplayNumbers.rate(payable.exchangeRate()) + ".");
        }
    }

    /** Keeps the difference in step with the amount typed in the payment dialog. */
    public void follow(MoneyField amountField) {
        Runnable sync = () -> updateAmount(parse(amountField.getText()));
        amountField.getDocument().addDocumentListener(onChange(sync));
        sync.run();
    }

    public void updateAmount(BigDecimal value) {
        amount = value;
        refreshDifference();
    }

    /** Payment rate for a foreign purchase; null for the base currency or a blank rate (the backend proposes it). */
    public BigDecimal exchangeRate() {
        if (!isForeign() || rateField.getText().isBlank()) return null;
        return rateField.value();
    }

    public JTextField rateField() { return rateField; }
    public JLabel difference() { return difference; }

    public static Preview preview(PayableDTO payable, BigDecimal amount, BigDecimal paymentRate) {
        BigDecimal treasury = money(amount.multiply(paymentRate));
        BigDecimal reduction;
        if (amount.compareTo(payable.outstanding()) >= 0) {
            reduction = baseOutstanding(payable);
        } else {
            BigDecimal paid = payable.amountPaid() == null ? BigDecimal.ZERO : payable.amountPaid();
            reduction = money(paid.add(amount).multiply(payable.exchangeRate()))
                    .subtract(money(paid.multiply(payable.exchangeRate())));
        }
        return new Preview(treasury, reduction, treasury.subtract(reduction));
    }

    private void refreshDifference() {
        BigDecimal rate = rateField.getText().isBlank() ? null : parse(rateField.getText());
        if (!isForeign() || rate == null || rate.signum() <= 0 || amount == null || amount.signum() <= 0) {
            difference.setText(" ");
            return;
        }
        Preview p = preview(payable, amount, rate);
        String flows = "Sai da tesouraria " + DisplayNumbers.money(p.treasuryAmount()) + " · baixa da dívida "
                + DisplayNumbers.money(p.supplierReduction()) + " · ";
        int sign = p.difference().signum();
        if (sign == 0) {
            difference.setText(flows + "Sem diferença cambial");
            difference.setForeground(UIHelper.TEXT_MUTED);
        } else if (sign > 0) {
            difference.setText(flows + "Perda cambial (6601): " + DisplayNumbers.money(p.difference()));
            difference.setForeground(UIHelper.REJECTED_RED);
        } else {
            difference.setText(flows + "Ganho cambial (7601): " + DisplayNumbers.money(p.difference().negate()));
            difference.setForeground(UIHelper.APPROVED_GREEN);
        }
    }

    private static BigDecimal baseOutstanding(PayableDTO payable) {
        if (payable.baseOutstanding() != null) return payable.baseOutstanding();
        return money(payable.outstanding().multiply(payable.exchangeRate()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal parse(String text) {
        try {
            return new BigDecimal(MoneyField.normalize(text));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}

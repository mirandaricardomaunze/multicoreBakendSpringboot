package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.gui.components.DecimalField;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.currency.dto.ProposedExchangeRateDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Currency and exchange rate of a purchase order or purchase (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3, §4.5).
 * The base currency always has rate 1 and sends no rate; a foreign currency gets the rate proposed by the table for
 * today, which the user may change. A blank rate is sent as empty, so the backend proposes it or asks for one.
 */
public final class DocumentCurrencyField extends JPanel {

    private static final int RATE_SCALE = 4;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final JComboBox<String> currencyCombo;
    private final DecimalField rateField = new DecimalField("", RATE_SCALE, true);
    private final JLabel hint = new JLabel(" ");
    private String baseCurrency = CurrencyCode.DEFAULT_CODE;
    private boolean locked;
    private boolean applying;
    private Runnable onCurrencyChange = () -> { };

    /** @param exchangeRateApiClient source of the proposed rate and of the base currency; null proposes nothing. */
    public DocumentCurrencyField(ExchangeRateApiClient exchangeRateApiClient) {
        super(new BorderLayout(8, 2));
        this.exchangeRateApiClient = exchangeRateApiClient;
        setOpaque(false);
        currencyCombo = UIHelper.createComboBox(
                Arrays.stream(CurrencyCode.values()).map(Enum::name).toArray(String[]::new), code -> code);
        currencyCombo.setPreferredSize(new Dimension(90, UIHelper.FORM_CONTROL_HEIGHT));
        currencyCombo.addActionListener(event -> currencyChanged());
        rateField.setToolTipText("Câmbio: quanto vale 1 unidade da moeda do documento na moeda base (até 4 casas).");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);
        add(currencyCombo, BorderLayout.WEST);
        add(rateField, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);
        currencyChanged();
    }

    /** New document: back to the company's base currency, editable. */
    public void reset() {
        locked = false;
        select(baseCurrency);
        if (exchangeRateApiClient != null) {
            UIHelper.loadAsync(this, exchangeRateApiClient::baseCurrency,
                    base -> { if (base != null) applyBaseCurrency(base.currency()); }, error -> { });
        }
    }

    /** Saved document: shows the currency and rate it was recorded with, without editing. */
    public void showDocument(String currency, BigDecimal rate) {
        locked = true;
        applying = true;
        currencyCombo.setSelectedItem(currency == null || currency.isBlank() ? baseCurrency : currency);
        applying = false;
        rateField.setText(DisplayNumbers.rate(rate == null ? BigDecimal.ONE : rate));
        hint.setText("Câmbio gravado no documento.");
        refreshState();
        onCurrencyChange.run();
    }

    public void applyBaseCurrency(String code) {
        if (code == null || code.isBlank() || code.equals(baseCurrency)) return;
        boolean wasBase = !isForeign();
        baseCurrency = code;
        if (wasBase && !locked) select(code);
        else refreshState();
    }

    /** Shows the proposed rate, ignoring a proposal for a currency no longer selected. */
    public void applyProposal(ProposedExchangeRateDTO proposal) {
        if (proposal == null || locked || !currency().equals(proposal.currency())) return;
        if (proposal.manualRateRequired() || proposal.rate() == null) {
            rateField.setText("");
            hint.setText("Sem câmbio na tabela: escreva o câmbio.");
        } else {
            rateField.setText(DisplayNumbers.rate(proposal.rate()));
            hint.setText("Câmbio de " + DATE.format(proposal.rateDate()) + " (pode alterar).");
        }
    }

    public void onCurrencyChange(Runnable listener) {
        this.onCurrencyChange = listener == null ? () -> { } : listener;
    }

    public String currency() {
        Object selected = currencyCombo.getSelectedItem();
        return selected == null ? baseCurrency : selected.toString();
    }

    public boolean isForeign() {
        return !currency().equals(baseCurrency);
    }

    /** Rate typed or proposed for a foreign currency; null for the base currency or a blank rate. */
    public BigDecimal exchangeRate() {
        if (!isForeign() || rateField.getText().isBlank()) return null;
        return rateField.value();
    }

    public JComboBox<String> currencyCombo() { return currencyCombo; }
    public JTextField rateField() { return rateField; }
    public JLabel hint() { return hint; }

    private void select(String code) {
        applying = true;
        currencyCombo.setSelectedItem(code);
        applying = false;
        currencyChanged();
    }

    private void currencyChanged() {
        if (applying) return;
        refreshState();
        if (isForeign()) {
            rateField.setText("");
            hint.setText(" ");
            proposeRate(currency());
        } else {
            rateField.setText(DisplayNumbers.rate(BigDecimal.ONE));
            hint.setText("Moeda base: câmbio 1.");
        }
        onCurrencyChange.run();
    }

    private void proposeRate(String code) {
        if (exchangeRateApiClient == null) return;
        UIHelper.loadAsync(this, () -> exchangeRateApiClient.propose(code, LocalDate.now()), this::applyProposal,
                error -> { if (code.equals(currency())) hint.setText("Câmbio proposto indisponível: escreva o câmbio."); });
    }

    private void refreshState() {
        currencyCombo.setEnabled(!locked);
        rateField.setEnabled(!locked && isForeign());
    }
}

package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * New or corrected entry of the exchange-rate table (SPEC §3.2). A new rate picks a foreign currency, the date
 * and the rate; a correction only changes the rate, because currency and date are its key. The rules (4 decimals,
 * &gt; 0, one per currency and date) are the backend's; its message is shown in the dialog when refused.
 */
final class ExchangeRateFormDialog {

    private static final int RATE_SCALE = 4;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final String baseCurrency;
    private final ExchangeRateDTO existing;

    ExchangeRateFormDialog(ExchangeRateApiClient exchangeRateApiClient, String baseCurrency, ExchangeRateDTO existing) {
        this.exchangeRateApiClient = exchangeRateApiClient;
        this.baseCurrency = baseCurrency;
        this.existing = existing;
    }

    /** Opens the dialog; true when a rate was saved. */
    boolean show() {
        return existing == null ? showNew() : showCorrection();
    }

    private boolean showNew() {
        JComboBox<String> currency = UIHelper.createComboBox(Arrays.stream(CurrencyCode.values())
                .map(Enum::name).filter(code -> !code.equals(baseCurrency)).toArray(String[]::new), code -> code);
        DateField date = new DateField(LocalDate.now());
        DecimalField rate = new DecimalField("", RATE_SCALE, true);
        JPanel form = UIHelper.createDialogForm("Moeda:", currency, "Data:", date,
                "", new FormField("Câmbio (" + baseCurrency + " por 1 unidade)", rate, true, "Até 4 casas decimais."));

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Novo câmbio", "fas-plus",
                "Tabela de câmbios da empresa activa", form);
        dialog.setOnSaveAsync(() -> {
            String code = String.valueOf(currency.getSelectedItem());
            LocalDate rateDate = date.value();
            BigDecimal value = rate.value();
            return () -> exchangeRateApiClient.create(code, rateDate, value);
        });
        return dialog.showDialog();
    }

    private boolean showCorrection() {
        DecimalField rate = new DecimalField(DisplayNumbers.rate(existing.rate()), RATE_SCALE, true);
        JPanel form = UIHelper.createDialogForm("Moeda:", readOnly(existing.currency()),
                "Data:", readOnly(DATE.format(existing.rateDate())),
                "", new FormField("Câmbio (" + baseCurrency + " por 1 unidade)", rate, true,
                        "Documentos já gravados mantêm o câmbio que usaram."));

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Corrigir câmbio", "fas-edit",
                existing.currency() + " de " + DATE.format(existing.rateDate()), form);
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = rate.value();
            Long id = existing.id();
            return () -> exchangeRateApiClient.correct(id, value);
        });
        return dialog.showDialog();
    }

    private static JTextField readOnly(String text) {
        JTextField field = new JTextField(text);
        UIHelper.styleTextField(field);
        UIHelper.setReadOnly(field, true);
        return field;
    }
}

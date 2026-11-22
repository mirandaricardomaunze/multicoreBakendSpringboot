package mz.multicore.erp.gui.currency;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.desktop.client.ExchangeRateApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.currency.dto.BaseCurrencyDTO;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configurações › Câmbios e Moeda Base (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.1, §3.2, §4.5): the daily
 * exchange-rate table of the active company and its base currency, read-only once the company has movements.
 * Creating and correcting rates and changing the base currency is ADMIN only; the backend decides, this only
 * hides what the server would refuse.
 */
public class ExchangeRatesPanel extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final DefaultTableModel ratesModel = new DefaultTableModel(new String[]{"Data", "Moeda", "Câmbio"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable ratesTable = new JTable(ratesModel);
    private final List<ExchangeRateDTO> rates = new ArrayList<>();
    private final JComboBox<String> baseCurrencyCombo = UIHelper.createComboBox(
            Arrays.stream(CurrencyCode.values()).map(Enum::name).toArray(String[]::new), code -> code);
    private final ModernButton saveBaseCurrencyBtn = UIHelper.createSuccessButton("Guardar moeda base");
    private final JLabel baseCurrencyStatus = new JLabel();
    private final ModernButton newRateBtn = UIHelper.createSuccessButton("Novo câmbio");
    private final ModernButton correctRateBtn = UIHelper.createSecondaryButton("Corrigir câmbio");
    private String baseCurrency = CurrencyCode.DEFAULT_CODE;

    public ExchangeRatesPanel(ExchangeRateApiClient exchangeRateApiClient) {
        this.exchangeRateApiClient = exchangeRateApiClient;
        setLayout(new BorderLayout(0, Spacing.LG));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));
        add(buildBaseCurrencyCard(), BorderLayout.NORTH);
        add(buildRatesCard(), BorderLayout.CENTER);
        applyPermissions(false);
    }

    private JComponent buildBaseCurrencyCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));
        card.add(new SectionHeader("Moeda base da empresa", "fas-coins"), BorderLayout.NORTH);

        saveBaseCurrencyBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBaseCurrencyBtn.setToolTipText("Stock, custo médio e contabilidade ficam sempre na moeda base");
        saveBaseCurrencyBtn.addActionListener(e -> saveBaseCurrency());
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Spacing.SM, 0));
        row.setOpaque(false);
        row.add(baseCurrencyCombo);
        row.add(saveBaseCurrencyBtn);
        card.add(row, BorderLayout.CENTER);

        baseCurrencyStatus.setFont(Typography.CAPTION);
        baseCurrencyStatus.setForeground(UIHelper.TEXT_MUTED);
        card.add(baseCurrencyStatus, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildRatesCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, Spacing.SM));
        card.setBorder(new EmptyBorder(Spacing.XL, Spacing.XL, Spacing.XL, Spacing.XL));

        UIHelper.styleTable(ratesTable);
        UIHelper.ensureHeadersFit(ratesTable);
        JScrollPane scroll = new JScrollPane(ratesTable);
        UIHelper.styleScrollPane(scroll);

        newRateBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newRateBtn.addActionListener(e -> openRateDialog(null));
        correctRateBtn.setIcon(UIHelper.icon("fas-edit", 14));
        correctRateBtn.addActionListener(e -> correctSelected());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);

        JLabel hint = new JLabel("Câmbio = unidades da moeda base por 1 unidade da moeda estrangeira. "
                + "Corrigir um câmbio não altera documentos já gravados.");
        hint.setFont(Typography.CAPTION);
        hint.setForeground(UIHelper.TEXT_MUTED);

        card.add(UIHelper.tableCardTop("Tabela de câmbios", hint, newRateBtn, correctRateBtn, refreshBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void refresh() {
        UIHelper.loadAsync(this, exchangeRateApiClient::baseCurrency, this::applyBaseCurrency,
                error -> showError("carregar a moeda base", error));
        UIHelper.loadAsync(this, exchangeRateApiClient::list, this::applyRates,
                error -> showError("carregar a tabela de câmbios", error));
    }

    void applyBaseCurrency(BaseCurrencyDTO dto) {
        if (dto == null) return;
        baseCurrency = dto.currency();
        baseCurrencyCombo.setSelectedItem(dto.currency());
        baseCurrencyStatus.setText(dto.editable()
                ? "Pode ser alterada enquanto a empresa não tiver documentos nem movimentos."
                : "Só leitura: a empresa já tem documentos ou movimentos registados em " + dto.currency() + ".");
        applyPermissions(dto.editable());
    }

    void applyRates(List<ExchangeRateDTO> list) {
        rates.clear();
        if (list != null) rates.addAll(list);
        ratesModel.setRowCount(0);
        for (ExchangeRateDTO rate : rates) {
            ratesModel.addRow(new Object[]{DATE.format(rate.rateDate()), rate.currency(), DisplayNumbers.rate(rate.rate())});
        }
    }

    private void applyPermissions(boolean baseCurrencyEditable) {
        boolean admin = SignedInUser.isAdmin();
        baseCurrencyCombo.setEnabled(admin && baseCurrencyEditable);
        saveBaseCurrencyBtn.setEnabled(admin && baseCurrencyEditable);
        newRateBtn.setEnabled(admin);
        correctRateBtn.setEnabled(admin);
    }

    private void saveBaseCurrency() {
        String chosen = String.valueOf(baseCurrencyCombo.getSelectedItem());
        UIHelper.loadAsync(this, () -> exchangeRateApiClient.changeBaseCurrency(chosen), dto -> {
            applyBaseCurrency(dto);
            ToastManager.show(this, FeedbackType.SUCCESS, "Moeda base da empresa: " + dto.currency() + ".");
        }, error -> showError("alterar a moeda base", error));
    }

    private void correctSelected() {
        int row = ratesTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione o câmbio a corrigir.");
            return;
        }
        openRateDialog(rates.get(ratesTable.convertRowIndexToModel(row)));
    }

    private void openRateDialog(ExchangeRateDTO existing) {
        if (new ExchangeRateFormDialog(exchangeRateApiClient, baseCurrency, existing).show()) {
            refresh();
        }
    }

    private void showError(String action, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR, "Não foi possível " + action + ": " + error.getMessage());
    }

    JTable ratesTable() { return ratesTable; }
    JComboBox<String> baseCurrencyCombo() { return baseCurrencyCombo; }
    ModernButton saveBaseCurrencyButton() { return saveBaseCurrencyBtn; }
    JLabel baseCurrencyStatus() { return baseCurrencyStatus; }
    ModernButton newRateButton() { return newRateBtn; }
    ModernButton correctRateButton() { return correctRateBtn; }
}

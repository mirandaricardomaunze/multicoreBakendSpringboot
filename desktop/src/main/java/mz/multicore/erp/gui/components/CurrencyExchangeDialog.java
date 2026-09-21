package mz.multicore.erp.gui.components;

import mz.multicore.erp.gui.components.MultiCurrencyEngine.Currency;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Diálogo de conversão e pagamento em moeda estrangeira no POS.
 */
public class CurrencyExchangeDialog extends JDialog {

    public record CurrencyPaymentResult(
            Currency currency,
            BigDecimal rate,
            BigDecimal foreignAmount,
            BigDecimal mznEquivalent,
            BigDecimal changeMzn
    ) {}

    private final BigDecimal totalMzn;
    private final Consumer<CurrencyPaymentResult> onConfirm;
    private JComboBox<Currency> currencyCombo;
    private JLabel totalForeignLabel;
    private JLabel rateLabel;
    private JTextField receivedField;
    private JLabel changeMznLabel;
    private JLabel receivedMznLabel;
    private ModernButton confirmBtn;

    public CurrencyExchangeDialog(Window owner, BigDecimal totalMzn, Consumer<CurrencyPaymentResult> onConfirm) {
        super(owner, "Pagamento Multimoeda / Câmbio", ModalityType.APPLICATION_MODAL);
        this.totalMzn = totalMzn == null ? BigDecimal.ZERO : totalMzn;
        this.onConfirm = onConfirm;

        setSize(480, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        initComponents();
        recalculate();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Cabeçalho
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = UIHelper.createHeading("Pagamento em Moeda Estrangeira");
        JLabel sub = new JLabel("Total da Venda: " + MultiCurrencyEngine.formatCurrency(totalMzn, Currency.MZN));
        sub.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        sub.setForeground(UIHelper.ACCENT_BLUE);
        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Formulário em Card
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Seletor de Moeda
        gbc.gridy = 0;
        card.add(createFieldTitle("Selecionar Moeda Estrangeira:"), gbc);

        gbc.gridy = 1;
        currencyCombo = new JComboBox<>(new Currency[]{Currency.USD, Currency.ZAR, Currency.EUR});
        currencyCombo.setFont(new Font(UIHelper.FONT, Font.PLAIN, 14));
        currencyCombo.setBackground(UIHelper.FIELD_BG);
        currencyCombo.setForeground(UIHelper.TEXT_LIGHT);
        currencyCombo.addActionListener(e -> recalculate());
        card.add(currencyCombo, gbc);

        // Taxa de Câmbio
        gbc.gridy = 2;
        rateLabel = new JLabel("Câmbio: 1 USD = 63.83 MT");
        rateLabel.setFont(new Font(UIHelper.FONT, Font.ITALIC, 12));
        rateLabel.setForeground(UIHelper.TEXT_MUTED);
        card.add(rateLabel, gbc);

        // Total Convertido
        gbc.gridy = 3;
        gbc.insets = new Insets(12, 4, 4, 4);
        card.add(createFieldTitle("Valor a Pagar na Moeda Selecionada:"), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(2, 4, 12, 4);
        totalForeignLabel = new JLabel("$ 0.00");
        totalForeignLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        totalForeignLabel.setForeground(UIHelper.PENDING_YELLOW);
        card.add(totalForeignLabel, gbc);

        // Valor Recebido
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 4, 4, 4);
        card.add(createFieldTitle("Montante Entregue pelo Cliente:"), gbc);

        gbc.gridy = 6;
        receivedField = new JTextField();
        receivedField.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        UIHelper.styleTextField(receivedField);
        receivedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalculate(); }
            @Override public void removeUpdate(DocumentEvent e) { recalculate(); }
            @Override public void changedUpdate(DocumentEvent e) { recalculate(); }
        });
        card.add(receivedField, gbc);

        // Equivalente recebido em MZN
        gbc.gridy = 7;
        receivedMznLabel = new JLabel("Equivalente: 0.00 MT");
        receivedMznLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        receivedMznLabel.setForeground(UIHelper.TEXT_MUTED);
        card.add(receivedMznLabel, gbc);

        // Troco em Meticais
        gbc.gridy = 8;
        gbc.insets = new Insets(12, 4, 4, 4);
        card.add(createFieldTitle("Troco a Devolver em Meticais (MT):"), gbc);

        gbc.gridy = 9;
        changeMznLabel = new JLabel("0.00 MT");
        changeMznLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        changeMznLabel.setForeground(UIHelper.APPROVED_GREEN);
        card.add(changeMznLabel, gbc);

        root.add(card, BorderLayout.CENTER);

        // Botões de Ação
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        ModernButton cancelBtn = UIHelper.createSecondaryButton("Cancelar");
        cancelBtn.addActionListener(e -> dispose());
        confirmBtn = UIHelper.createPrimaryButton("Confirmar Pagamento");
        confirmBtn.setIcon(UIHelper.icon("fas-check", 14));
        confirmBtn.addActionListener(e -> onConfirmClicked());

        buttons.add(cancelBtn);
        buttons.add(confirmBtn);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createFieldTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        l.setForeground(UIHelper.TEXT_LIGHT);
        return l;
    }

    private void recalculate() {
        Currency curr = (Currency) currencyCombo.getSelectedItem();
        if (curr == null) curr = Currency.USD;

        BigDecimal rate = MultiCurrencyEngine.getExchangeRate(curr);
        rateLabel.setText("Taxa de Câmbio: 1 " + curr.name() + " = " + rate.toPlainString() + " MT");

        BigDecimal totalForeign = MultiCurrencyEngine.convertToForeign(totalMzn, curr);
        totalForeignLabel.setText(MultiCurrencyEngine.formatCurrency(totalForeign, curr));

        String input = receivedField.getText().trim().replace(",", ".");
        BigDecimal receivedForeign = BigDecimal.ZERO;
        try {
            if (!input.isBlank()) {
                receivedForeign = new BigDecimal(input);
            }
        } catch (NumberFormatException ignored) {}

        BigDecimal receivedMzn = MultiCurrencyEngine.convertToMzn(receivedForeign, curr);
        receivedMznLabel.setText("Entregue em MT: " + MultiCurrencyEngine.formatCurrency(receivedMzn, Currency.MZN));

        BigDecimal changeMzn = MultiCurrencyEngine.calculateChangeInMzn(totalMzn, receivedForeign, curr);
        changeMznLabel.setText(MultiCurrencyEngine.formatCurrency(changeMzn, Currency.MZN));

        boolean canConfirm = receivedMzn.compareTo(totalMzn) >= 0 && totalMzn.signum() > 0;
        confirmBtn.setEnabled(canConfirm);
    }

    private void onConfirmClicked() {
        Currency curr = (Currency) currencyCombo.getSelectedItem();
        if (curr == null) curr = Currency.USD;
        String input = receivedField.getText().trim().replace(",", ".");
        BigDecimal receivedForeign = BigDecimal.ZERO;
        try {
            if (!input.isBlank()) {
                receivedForeign = new BigDecimal(input);
            }
        } catch (Exception ignored) {}

        BigDecimal rate = MultiCurrencyEngine.getExchangeRate(curr);
        BigDecimal receivedMzn = MultiCurrencyEngine.convertToMzn(receivedForeign, curr);
        BigDecimal changeMzn = MultiCurrencyEngine.calculateChangeInMzn(totalMzn, receivedForeign, curr);

        if (onConfirm != null) {
            onConfirm.accept(new CurrencyPaymentResult(curr, rate, receivedForeign, receivedMzn, changeMzn));
        }
        dispose();
    }
}

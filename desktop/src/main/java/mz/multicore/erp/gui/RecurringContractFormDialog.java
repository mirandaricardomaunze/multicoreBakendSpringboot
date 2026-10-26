package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.RecurringContractApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;

import javax.swing.*;
import java.awt.Component;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Diálogo modal ergonómico para registo de novos contratos de avença recorrente. */
public final class RecurringContractFormDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RecurringContractFormDialog() {}

    public static boolean show(Component parent, RecurringContractApiClient client) {
        Window owner = SwingUtilities.getWindowAncestor(parent);

        JTextField numberField = new JTextField();
        JTextField customerIdField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<BillingFrequency> freqCombo = UIHelper.createComboBox(BillingFrequency.values(), BillingFrequency::label);

        Integer[] days = new Integer[31];
        for (int i = 1; i <= 31; i++) days[i - 1] = i;
        JComboBox<Integer> dayCombo = UIHelper.createComboBox(days, d -> "Dia " + d);

        JTextField amountField = new JTextField();
        JTextField startDateField = new JTextField(LocalDate.now().format(DATE_FMT));
        JTextField endDateField = new JTextField();
        JCheckBox autoRenewCheck = new JCheckBox("Renovação Automática do Contrato", true);
        autoRenewCheck.setOpaque(false);
        autoRenewCheck.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Nº do Contrato *:", numberField,
                "ID do Cliente *:", customerIdField,
                "Descrição do Serviço *:", descField,
                "Periodicidade *:", freqCombo,
                "Dia de Cobrança (1-31) *:", dayCombo,
                "Valor Fixo (MT) *:", amountField,
                "Data de Início (DD/MM/AAAA) *:", startDateField,
                "Data de Término (Opcional):", endDateField,
                "", autoRenewCheck
        );

        ModernFormDialog dialog = new ModernFormDialog(
                owner,
                "Registar Novo Contrato Recorrente",
                "fas-file-contract",
                "Adicionar contrato de avença com facturação periódica",
                form
        );

        dialog.setOnSave(() -> {
            String number = numberField.getText().trim();
            if (number.isEmpty()) {
                numberField.requestFocus();
                throw new IllegalArgumentException("O número do contrato é obrigatório.");
            }

            Long customerId;
            try {
                customerId = Long.parseLong(customerIdField.getText().trim());
            } catch (NumberFormatException e) {
                customerIdField.requestFocus();
                throw new IllegalArgumentException("O ID do cliente deve ser um número válido.");
            }

            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                descField.requestFocus();
                throw new IllegalArgumentException("A descrição do serviço/avença é obrigatória.");
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountField.getText().trim().replace(",", "."));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    amountField.requestFocus();
                    throw new IllegalArgumentException("O valor fixo deve ser positivo.");
                }
            } catch (Exception e) {
                amountField.requestFocus();
                throw new IllegalArgumentException("Valor fixo inválido.");
            }

            LocalDate startDate;
            try {
                startDate = LocalDate.parse(startDateField.getText().trim(), DATE_FMT);
            } catch (Exception e) {
                startDateField.requestFocus();
                throw new IllegalArgumentException("Data de início inválida. Use DD/MM/AAAA.");
            }

            LocalDate endDate = null;
            String endStr = endDateField.getText().trim();
            if (!endStr.isEmpty()) {
                try {
                    endDate = LocalDate.parse(endStr, DATE_FMT);
                    if (endDate.isBefore(startDate)) {
                        endDateField.requestFocus();
                        throw new IllegalArgumentException("A data de término não pode ser anterior à data de início.");
                    }
                } catch (Exception e) {
                    endDateField.requestFocus();
                    throw new IllegalArgumentException("Data de término inválida. Use DD/MM/AAAA.");
                }
            }

            BillingFrequency freq = (BillingFrequency) freqCombo.getSelectedItem();
            int day = (Integer) dayCombo.getSelectedItem();

            CreateRecurringContractRequest req = new CreateRecurringContractRequest(
                    number, customerId, desc, freq, day, amount, startDate, endDate, autoRenewCheck.isSelected()
            );
            client.createContract(req);
        });

        return dialog.showDialog();
    }
}

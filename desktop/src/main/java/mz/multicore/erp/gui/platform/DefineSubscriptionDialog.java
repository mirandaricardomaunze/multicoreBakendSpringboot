package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;

import javax.swing.*;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Diálogo especializado para definição de plano, preço mensal e validade de assinatura.
 */
public class DefineSubscriptionDialog {

    public static boolean show(Window owner, PlatformApiClient platformApiClient, SubscriptionDTO sub) {
        JComboBox<String> planCombo = new JComboBox<>(new String[]{"TRIAL", "BASIC", "PRO", "ENTERPRISE"});
        UIHelper.styleComboBox(planCombo);
        if (sub.plan() != null) planCombo.setSelectedItem(sub.plan());
        JTextField priceField = new JTextField(sub.monthlyPrice() == null ? "" : sub.monthlyPrice().toPlainString());
        DateField validField = new DateField(sub.validUntil());
        UIHelper.styleTextField(priceField);

        JPanel form = UIHelper.createDialogForm(
                "Plano:", planCombo,
                "Preço mensal (MT):", priceField,
                "Válida até (AAAA-MM-DD):", validField
        );

        ModernFormDialog dlg = new ModernFormDialog(owner, "Definir Assinatura",
                "fas-sliders-h", sub.companyName(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            SaveSubscriptionRequest request = new SaveSubscriptionRequest((String) planCombo.getSelectedItem(),
                    parseAmount(priceField.getText(), "preço mensal"), parseDate(validField.getText()));
            return () -> platformApiClient.saveSubscription(sub.companyId(), request);
        });

        return dlg.showDialog();
    }

    private static BigDecimal parseAmount(String text, String field) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para " + field + ".");
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }
}

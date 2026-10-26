package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Diálogo modal para criação e edição de clientes comerciais.
 */
public final class ClientFormDialog {

    private ClientFormDialog() {
    }

    public static void show(Component parent, ClientDTO existing, ComercialApiClient client, Runnable onSuccess) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField taxIdField = new JTextField(existing == null ? "" : existing.taxId());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField addressField = new JTextField(existing == null || existing.address() == null ? "" : existing.address());
        IntegerField termsField = new IntegerField(
                String.valueOf(existing == null ? 0 : existing.paymentTermsDays()), 0, 365, "O prazo de pagamento");
        MoneyField creditField = new MoneyField(existing == null || existing.creditLimit() == null
                ? "" : existing.creditLimit().toPlainString());

        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.installDigitsOnlyFilter(taxIdField, 9);

        FormField nameForm = new FormField("Nome", nameField, true, "Nome completo ou denominação social");
        FormField taxForm = new FormField("NUIT / NIF", taxIdField, true, null);
        FormField emailForm = new FormField("Email", emailField, true, null);
        FormField addressForm = new FormField("Endereço", addressField, false, null);
        FormField termsForm = new FormField("Prazo de pagamento (dias)", termsField, false,
                "0 = pronto pagamento. Define o vencimento das faturas futuras deste cliente.");
        FormField creditForm = new FormField("Limite de crédito (MT)", creditField, false,
                "Em branco = sem limite. Zero = não vende a crédito.");

        JPanel form = UIHelper.createDialogForm(
                "", nameForm, "", taxForm, "", emailForm, "", addressForm, "", termsForm, "", creditForm);

        String title = existing == null ? "Novo Cliente" : "Editar Cliente — " + existing.name();
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, title, null, "Dados de cadastro do cliente", form);
        dialog.setOnSaveAsync(() -> {
            boolean valid = nameForm.validateMinLength(3, "Nome")
                    & taxForm.validateNuit()
                    & emailForm.validateEmail();
            if (!valid) throw new IllegalArgumentException("Corrija os campos assinalados com formato inválido.");

            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            int terms = termsField.value();
            BigDecimal creditLimit = creditField.optionalValue();

            return () -> {
                if (existing == null) {
                    client.createClient(name, taxId, email, address, terms, creditLimit);
                } else {
                    client.updateClient(existing.id(), name, taxId, email, address, terms, creditLimit);
                }
                return null;
            };
        });

        if (dialog.showDialog()) {
            ToastManager.success(parent, existing == null ? "Cliente criado." : "Cliente actualizado.");
            if (onSuccess != null) onSuccess.run();
        }
    }
}

package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.FormField;
import mz.multicore.erp.gui.components.IntegerField;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import java.math.BigDecimal;

/**
 * Campos e validação do formulário de cliente, separados do diálogo para serem testáveis.
 * O email é opcional (docs/IMPORTACAO_DADOS_SPEC.md §4.1): vazio é aceite e enviado como {@code null}.
 */
final class ClientFormFields {

    final JTextField nameField;
    final JTextField taxIdField;
    final JTextField emailField;
    final JTextField addressField;
    final IntegerField termsField;
    final MoneyField creditField;

    final FormField nameForm;
    final FormField taxForm;
    final FormField emailForm;
    private final FormField addressForm;
    private final FormField termsForm;
    private final FormField creditForm;

    ClientFormFields(ClientDTO existing) {
        nameField = new JTextField(existing == null ? "" : existing.name());
        taxIdField = new JTextField(existing == null ? "" : existing.taxId());
        emailField = new JTextField(existing == null || existing.email() == null ? "" : existing.email());
        addressField = new JTextField(existing == null || existing.address() == null ? "" : existing.address());
        termsField = new IntegerField(
                String.valueOf(existing == null ? 0 : existing.paymentTermsDays()), 0, 365, "O prazo de pagamento");
        creditField = new MoneyField(existing == null || existing.creditLimit() == null
                ? "" : existing.creditLimit().toPlainString());

        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.installDigitsOnlyFilter(taxIdField, 9);

        nameForm = new FormField("Nome", nameField, true, "Nome completo ou denominação social");
        taxForm = new FormField("NUIT / NIF", taxIdField, true, null);
        emailForm = new FormField("Email", emailField, false, "Opcional.");
        addressForm = new FormField("Endereço", addressField, false, null);
        termsForm = new FormField("Prazo de pagamento (dias)", termsField, false,
                "0 = pronto pagamento. Define o vencimento das faturas futuras deste cliente.");
        creditForm = new FormField("Limite de crédito (MT)", creditField, false,
                "Em branco = sem limite. Zero = não vende a crédito.");
    }

    JPanel form() {
        return UIHelper.createDialogForm(
                "", nameForm, "", taxForm, "", emailForm, "", addressForm, "", termsForm, "", creditForm);
    }

    /** Valida todos os campos (sem curto-circuito, para assinalar todos os erros de uma vez). */
    boolean validateAll() {
        return nameForm.validateMinLength(3, "Nome")
                & taxForm.validateNuit()
                & emailForm.validateEmail();
    }

    String name() {
        return nameField.getText().trim();
    }

    String taxId() {
        return taxIdField.getText().trim();
    }

    /** Email sem espaços, ou {@code null} quando o campo fica vazio. */
    String email() {
        String email = emailField.getText().trim();
        return email.isEmpty() ? null : email;
    }

    String address() {
        return addressField.getText().trim();
    }

    int paymentTermsDays() {
        return termsField.value();
    }

    BigDecimal creditLimit() {
        return creditField.optionalValue();
    }
}

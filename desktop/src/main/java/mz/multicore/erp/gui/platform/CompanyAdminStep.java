package mz.multicore.erp.gui.platform;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;

import javax.swing.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

import static mz.multicore.erp.gui.platform.CompanyFormFields.*;

/**
 * Last step of the registration wizard: the company's first administrator and its plan.
 * The password rules mirror the server's {@code PasswordPolicy} only to fail fast; the server
 * stays the authority.
 */
final class CompanyAdminStep {

    private static final String TRIAL = "TRIAL";

    final JTextField adminName = new JTextField();
    final JTextField username = new JTextField();
    final JPasswordField password = new JPasswordField();
    final JPasswordField passwordConfirm = new JPasswordField();
    final JComboBox<Choice<String>> plan = new JComboBox<>();
    final JTextField monthlyPrice = new JTextField("0");
    final DateField validUntil = new DateField(LocalDate.now().plusDays(OnboardCompanyRequest.DEFAULT_TRIAL_DAYS));

    CompanyAdminStep() {
        plan.addItem(new Choice<>(TRIAL, "Avaliação (" + OnboardCompanyRequest.DEFAULT_TRIAL_DAYS + " dias grátis)"));
        plan.addItem(new Choice<>("BASIC", "Básico"));
        plan.addItem(new Choice<>("PRO", "Profissional"));
        plan.addItem(new Choice<>("ENTERPRISE", "Empresarial"));
        plan.addActionListener(e -> onPlanChanged());
    }

    JPanel panel() {
        JPanel panel = column();
        panel.add(section("Administrador da empresa", UIHelper.createDialogForm(
                "Nome completo *", adminName,
                "Nome de utilizador *", username,
                "Senha *", password,
                "Confirmar senha *", passwordConfirm),
                "É o primeiro utilizador da empresa, com perfil de Administrador. A senha precisa de "
                        + "pelo menos 8 caracteres, com letras e números, e não pode conter o nome de utilizador."));
        panel.add(section("Assinatura", UIHelper.createDialogForm(
                "Plano", plan,
                "Preço mensal (MT)", monthlyPrice,
                "Válida até (AAAA-MM-DD)", validUntil),
                "Pode alterar o plano mais tarde no separador Assinaturas."));
        return panel;
    }

    /** Proposes "admin.<first word of the company>" when the username is still empty. */
    void suggestUsername(String companyName) {
        if (!username.getText().isBlank() || companyName == null) return;
        String ascii = Normalizer.normalize(companyName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String firstWord = ascii.replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+")[0];
        if (!firstWord.isEmpty()) username.setText("admin." + firstWord);
    }

    String validateStep() {
        clearInvalid(adminName, username, password, passwordConfirm, monthlyPrice, validUntil);
        if (adminName.getText().isBlank()) return invalid(adminName, "Indique o nome do administrador.");
        String user = username.getText().trim();
        if (user.length() < 3 || user.length() > 50 || user.contains(" ")) {
            return invalid(username, "O nome de utilizador deve ter entre 3 e 50 caracteres, sem espaços.");
        }
        String pass = new String(password.getPassword());
        if (pass.length() < 8 || !pass.matches(".*\\p{L}.*") || !pass.matches(".*\\d.*")) {
            return invalid(password, "A senha precisa de pelo menos 8 caracteres, com letras e números.");
        }
        if (pass.toLowerCase(Locale.ROOT).contains(user.toLowerCase(Locale.ROOT))) {
            return invalid(password, "A senha não pode conter o nome de utilizador.");
        }
        if (!pass.equals(new String(passwordConfirm.getPassword()))) {
            return invalid(passwordConfirm, "A confirmação não coincide com a senha.");
        }
        if (!monthlyPrice.getText().isBlank() && parseAmount(monthlyPrice.getText()) == null) {
            return invalid(monthlyPrice, "O preço mensal deve ser um valor, por exemplo 2 500,00.");
        }
        if (!validUntil.getText().isBlank() && validUntil.getDate() == null) {
            return invalid(validUntil, "Data inválida: use o formato AAAA-MM-DD.");
        }
        return null;
    }

    OnboardCompanyRequest.Administrator administrator() {
        return new OnboardCompanyRequest.Administrator(
                username.getText().trim(), adminName.getText().trim(), new String(password.getPassword()));
    }

    SaveSubscriptionRequest subscription() {
        String code = selected(plan);
        BigDecimal price = parseAmount(monthlyPrice.getText());
        return new SaveSubscriptionRequest(code == null ? TRIAL : code,
                price == null ? BigDecimal.ZERO : price, validUntil.getDate());
    }

    String planLabel() {
        Object item = plan.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    private void onPlanChanged() {
        if (TRIAL.equals(selected(plan))) {
            monthlyPrice.setText("0");
            validUntil.setDate(LocalDate.now().plusDays(OnboardCompanyRequest.DEFAULT_TRIAL_DAYS));
        } else if ("0".equals(monthlyPrice.getText().trim())) {
            monthlyPrice.setText("");
            validUntil.setDate(LocalDate.now().plusMonths(1));
        }
    }
}

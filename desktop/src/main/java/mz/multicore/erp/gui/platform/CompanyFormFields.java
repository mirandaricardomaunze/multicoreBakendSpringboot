package mz.multicore.erp.gui.platform;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.company.model.CompanyLegalForm;
import mz.multicore.erp.modules.company.model.MozambiqueProvince;
import mz.multicore.erp.modules.company.model.VatRegime;
import mz.multicore.erp.modules.platform.dto.CompanyProfile;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.platform.dto.UpdateCompanyRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Company fields of the registration/edit wizard, grouped by step, with the mapping to and from
 * the platform contracts. Client-side checks cover only what blocks moving to the next step
 * (required fields, NUIT shape); the server validates everything again.
 */
final class CompanyFormFields {

    /** Combo entry with a label; {@code value == null} is the "not stated" entry. */
    record Choice<E>(E value, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final boolean editing;

    final JTextField name = new JTextField();
    final JTextField tradeName = new JTextField();
    final JTextField taxId = new JTextField();
    final JComboBox<Choice<CompanyLegalForm>> legalForm =
            choices(CompanyLegalForm.values(), CompanyLegalForm::label, "Não indicada");
    final byte[][] logo = {null};

    final JTextField address = new JTextField();
    final JTextField neighborhood = new JTextField();
    final JTextField city = new JTextField();
    final JComboBox<Choice<MozambiqueProvince>> province =
            choices(MozambiqueProvince.values(), MozambiqueProvince::label, "Não indicada");
    final JTextField postalCode = new JTextField();
    final JTextField phone = new JTextField();
    final JTextField secondaryPhone = new JTextField();
    final JTextField email = new JTextField();
    final JTextField website = new JTextField();
    final JTextField contactName = new JTextField();
    final JTextField contactPhone = new JTextField();

    final JTextField registryNumber = new JTextField();
    final JTextField shareCapital = new JTextField();
    final JTextField activityLicense = new JTextField();
    final JTextField activityCode = new JTextField();
    final JComboBox<Choice<VatRegime>> vatRegime =
            choices(VatRegime.values(), VatRegime::label, "Não indicado");
    final JTextField taxOffice = new JTextField();

    final JTextField bankName = new JTextField();
    final JTextField bankAccountNumber = new JTextField();

    private final boolean hasExistingLogo;

    CompanyFormFields(PlatformCompanyDTO existing) {
        this.editing = existing != null;
        this.hasExistingLogo = existing != null && existing.hasLogo();
        UIHelper.installDigitsOnlyFilter(taxId, 9);
        UIHelper.installDigitsOnlyFilter(activityCode, 5);
        if (existing != null) fill(existing);
        taxId.setEditable(!editing);
        taxId.setEnabled(!editing);
        if (editing) taxId.setToolTipText("O NUIT não pode ser alterado depois do registo.");
    }

    // ------------------------------------------------------------------ step panels

    JPanel identityStep(Component dialogParent) {
        return section(null, UIHelper.createDialogForm(
                "Razão social *", name,
                "Nome comercial", tradeName,
                editing ? "NUIT" : "NUIT *", taxId,
                "Forma jurídica", legalForm,
                "Logótipo", LogoPicker.create(dialogParent, logo, hasExistingLogo)),
                "A razão social e o NUIT aparecem em todas as facturas. O nome comercial, quando "
                        + "indicado, é o título dos documentos.");
    }

    JPanel addressStep() {
        JPanel panel = column();
        panel.add(section("Morada", UIHelper.createDialogForm(
                "Endereço (rua, n.º)", address,
                "Bairro", neighborhood,
                "Cidade / localidade", city,
                "Província", province,
                "Código postal", postalCode), null));
        panel.add(section("Contactos", UIHelper.createDialogForm(
                "Telefone principal", phone,
                "Telefone alternativo", secondaryPhone,
                "Email", email,
                "Website", website,
                "Pessoa de contacto", contactName,
                "Telefone do contacto", contactPhone),
                "A pessoa de contacto é quem a plataforma contacta sobre a assinatura e o suporte."));
        return panel;
    }

    JPanel legalStep() {
        JPanel panel = column();
        panel.add(section("Registo e fiscalidade", UIHelper.createDialogForm(
                "Matrícula na Conservatória", registryNumber,
                "Capital social (MT)", shareCapital,
                "Alvará", activityLicense,
                "CAE (código da actividade)", activityCode,
                "Regime de IVA", vatRegime,
                "Repartição de finanças", taxOffice),
                "Estes dados saem no cabeçalho das facturas. Um regime de IVA diferente do normal "
                        + "é indicado no documento."));
        if (!editing) {
            panel.add(section("Conta bancária para as facturas (opcional)", UIHelper.createDialogForm(
                    "Banco", bankName,
                    "N.º de conta ou NIB", bankAccountNumber),
                    "Aparece nas facturas como dados para pagamento por transferência. Pode juntar "
                            + "outras contas mais tarde na tesouraria da empresa."));
        }
        return panel;
    }

    // ------------------------------------------------------------------ validation

    /** Checks the identity step; returns the first problem or {@code null}. Marks the bad field. */
    String validateIdentity() {
        clearInvalid(name, taxId);
        if (name.getText().isBlank()) return invalid(name, "Indique a razão social da empresa.");
        if (!editing && !taxId.getText().trim().matches("\\d{9}")) {
            return invalid(taxId, "O NUIT deve ter 9 dígitos.");
        }
        return null;
    }

    String validateLegal() {
        clearInvalid(shareCapital, bankName, bankAccountNumber);
        if (!shareCapital.getText().isBlank() && parseAmount(shareCapital.getText()) == null) {
            return invalid(shareCapital, "O capital social deve ser um valor, por exemplo 50 000,00.");
        }
        boolean hasBank = !bankName.getText().isBlank();
        boolean hasAccount = !bankAccountNumber.getText().isBlank();
        if (hasBank && !hasAccount) return invalid(bankAccountNumber, "Indique o número da conta ou o NIB.");
        if (hasAccount && !hasBank) return invalid(bankName, "Indique o banco da conta.");
        return null;
    }

    // ------------------------------------------------------------------ contracts

    CreateCompanyRequest toCreateRequest() {
        return new CreateCompanyRequest(text(name), text(taxId), text(email), text(address), text(phone), profile());
    }

    UpdateCompanyRequest toUpdateRequest() {
        return new UpdateCompanyRequest(text(name), text(email), text(address), text(phone), profile());
    }

    OnboardCompanyRequest.BankAccount bankAccountOrNull() {
        if (bankName.getText().isBlank() || bankAccountNumber.getText().isBlank()) return null;
        return new OnboardCompanyRequest.BankAccount(text(bankName), text(bankAccountNumber));
    }

    byte[] chosenLogo() {
        return logo[0];
    }

    String displayName() {
        String trade = text(tradeName);
        return trade != null ? trade : text(name);
    }

    private CompanyProfile profile() {
        return new CompanyProfile(
                text(tradeName), selected(legalForm), text(registryNumber), parseAmount(shareCapital.getText()),
                text(activityLicense), text(activityCode), selected(vatRegime), text(taxOffice),
                text(neighborhood), text(city), selected(province), text(postalCode),
                text(secondaryPhone), text(website), text(contactName), text(contactPhone));
    }

    private void fill(PlatformCompanyDTO c) {
        name.setText(nz(c.name()));
        taxId.setText(nz(c.taxId()));
        email.setText(nz(c.email()));
        address.setText(nz(c.address()));
        phone.setText(nz(c.phone()));
        CompanyProfile p = c.profile() == null ? CompanyProfile.empty() : c.profile();
        tradeName.setText(nz(p.tradeName()));
        select(legalForm, p.legalForm());
        registryNumber.setText(nz(p.registryNumber()));
        shareCapital.setText(p.shareCapital() == null ? "" : p.shareCapital().stripTrailingZeros().toPlainString());
        activityLicense.setText(nz(p.activityLicense()));
        activityCode.setText(nz(p.activityCode()));
        select(vatRegime, p.vatRegime());
        taxOffice.setText(nz(p.taxOffice()));
        neighborhood.setText(nz(p.neighborhood()));
        city.setText(nz(p.city()));
        select(province, p.province());
        postalCode.setText(nz(p.postalCode()));
        secondaryPhone.setText(nz(p.secondaryPhone()));
        website.setText(nz(p.website()));
        contactName.setText(nz(p.contactName()));
        contactPhone.setText(nz(p.contactPhone()));
    }

    // ------------------------------------------------------------------ helpers

    /** Accepts "50000", "50000.50", "50 000,50" and "50.000,50". Returns {@code null} when invalid or blank. */
    static BigDecimal parseAmount(String raw) {
        if (raw == null) return null;
        String s = raw.replace("MT", "").replace(" ", "").replace(" ", "").trim();
        if (s.isEmpty()) return null;
        if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        } else if (s.indexOf('.') != s.lastIndexOf('.')) {
            s = s.replace(".", "");
        }
        try {
            BigDecimal value = new BigDecimal(s);
            return value.signum() < 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static <E> JComboBox<Choice<E>> choices(E[] values, Function<E, String> label, String emptyLabel) {
        JComboBox<Choice<E>> combo = new JComboBox<>();
        combo.addItem(new Choice<>(null, emptyLabel));
        for (E value : values) combo.addItem(new Choice<>(value, label.apply(value)));
        return combo;
    }

    static <E> E selected(JComboBox<Choice<E>> combo) {
        Object item = combo.getSelectedItem();
        return item instanceof Choice<?> choice ? cast(choice.value()) : null;
    }

    @SuppressWarnings("unchecked")
    private static <E> E cast(Object value) {
        return (E) value;
    }

    static <E> void select(JComboBox<Choice<E>> combo, E value) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (java.util.Objects.equals(combo.getItemAt(i).value(), value)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    static JPanel column() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    /** Titled group of fields with an optional explanatory line underneath. */
    static JPanel section(String title, JPanel form, String hint) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(0, 0, 8, 0));
        if (title != null) {
            JLabel heading = new JLabel(title);
            heading.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
            heading.setForeground(UIHelper.TEXT_LIGHT);
            heading.setBorder(new EmptyBorder(4, 14, 0, 14));
            panel.add(heading, BorderLayout.NORTH);
        }
        form.setOpaque(false);
        panel.add(form, BorderLayout.CENTER);
        if (hint != null) {
            // A wrapping text area instead of an HTML label: an HTML label keeps its single-line
            // width and is cut when the dialog is narrower than the sentence.
            JTextArea hintText = new JTextArea(hint);
            hintText.setLineWrap(true);
            hintText.setWrapStyleWord(true);
            hintText.setEditable(false);
            hintText.setFocusable(false);
            hintText.setOpaque(false);
            hintText.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            hintText.setForeground(UIHelper.TEXT_MUTED);
            hintText.setBorder(new EmptyBorder(0, 21, 0, 21));
            panel.add(hintText, BorderLayout.SOUTH);
        }
        return panel;
    }

    static String invalid(JComponent field, String message) {
        UIHelper.markFieldInvalid(field, message);
        field.requestFocusInWindow();
        return message;
    }

    static void clearInvalid(JComponent... fields) {
        for (JComponent field : fields) UIHelper.clearFieldInvalid(field);
    }

    static String text(JTextField field) {
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}

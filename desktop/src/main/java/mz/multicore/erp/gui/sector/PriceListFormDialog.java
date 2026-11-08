package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pricing.dto.PriceListDTO;
import mz.multicore.erp.modules.pricing.dto.SavePriceListRequest;

import javax.swing.*;
import java.time.LocalDate;
import java.util.function.Consumer;

/** Creates or edits a price list: code, name, optional validity and status. */
final class PriceListFormDialog {

    private PriceListFormDialog() {
    }

    static void show(JComponent parent, PricingApiClient api, PriceListDTO existing, Consumer<PriceListDTO> onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;

        JTextField codeField = new JTextField(existing == null ? "" : existing.code());
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField descriptionField = new JTextField(existing == null || existing.description() == null ? "" : existing.description());
        DateField fromField = new DateField(existing == null ? null : existing.validFrom());
        DateField toField = new DateField(existing == null ? null : existing.validTo());
        if (existing == null || existing.validFrom() == null) fromField.setText("");
        if (existing == null || existing.validTo() == null) toField.setText("");
        JCheckBox activeBox = new JCheckBox("Tabela activa", existing == null || existing.active());
        activeBox.setOpaque(false);
        activeBox.setForeground(UIHelper.TEXT_LIGHT);
        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(descriptionField);

        JLabel hint = new JLabel("<html>Deixe as datas vazias para uma tabela sem prazo. Uma tabela inactiva "
                + "ou fora de prazo deixa de valer, e os clientes voltam ao preço normal.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);

        FormField codeForm = new FormField("Código", codeField, true, "Ex.: REVENDA, EMPREITEIRO");
        FormField nameForm = new FormField("Nome", nameField, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "", codeForm,
                "", nameForm,
                "Descrição:", descriptionField,
                "Válida desde (yyyy-MM-dd):", fromField,
                "Válida até (yyyy-MM-dd):", toField,
                "Estado:", activeBox,
                "", hint});

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Nova Tabela de Preço" : "Editar Tabela de Preço", "fas-tags",
                "Preços próprios para um tipo de cliente", form);
        PriceListDTO[] result = new PriceListDTO[1];
        dialog.setOnSaveAsync(() -> {
            boolean ok = codeForm.validateRequired() & nameForm.validateRequired();
            if (!ok) throw new IllegalArgumentException("Preencha o código e o nome da tabela.");
            LocalDate from = optionalDate(fromField);
            LocalDate to = optionalDate(toField);
            if (from != null && to != null && to.isBefore(from)) {
                UIHelper.markFieldInvalid(toField, "A data de fim não pode ser anterior à de início.");
                throw new IllegalArgumentException("A data de fim não pode ser anterior à de início.");
            }
            String description = descriptionField.getText().trim();
            SavePriceListRequest request = new SavePriceListRequest(existing == null ? null : existing.id(), companyId,
                    codeField.getText().trim(), nameField.getText().trim(), description.isEmpty() ? null : description,
                    from, to, activeBox.isSelected());
            return () -> { result[0] = api.save(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) onSaved.accept(result[0]);
    }

    private static LocalDate optionalDate(DateField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.isEmpty()) {
            UIHelper.clearFieldInvalid(field);
            return null;
        }
        return field.value();
    }
}

package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.OvertimeRateConfigDTO;
import mz.multicore.erp.modules.hr.dto.SaveOvertimeRateConfigRequest;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Diálogo modal de configuração dos acréscimos de hora extra. */
final class HrOvertimeRatesDialog {
    private HrOvertimeRatesDialog() {}

    static void show(HRPanel owner) {
        List<OvertimeRateConfigDTO> existing;
        try {
            existing = owner.hrApiClient.getOvertimeRates();
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
            return;
        }
        var current = existing.stream()
                .filter(OvertimeRateConfigDTO::active)
                .findFirst().orElse(null);

        JTextField nameField = new JTextField(current == null ? "Acréscimos de hora extra" : current.name());
        UIHelper.styleTextField(nameField);
        DateField fromField = new DateField(LocalDate.now().withDayOfMonth(1));
        JTextField dayField = new JTextField(current == null ? "" : current.dayMultiplier().toPlainString());
        UIHelper.styleTextField(dayField);
        JTextField nightField = new JTextField(current == null ? "" : current.nightMultiplier().toPlainString());
        UIHelper.styleTextField(nightField);
        JTextField restField = new JTextField(current == null ? "" : current.restDayMultiplier().toPlainString());
        UIHelper.styleTextField(restField);
        JTextField basisField = new JTextField(
                current == null || current.legalBasis() == null ? "" : current.legalBasis());
        UIHelper.styleTextField(basisField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Vigora a partir de:", fromField,
                "Extra diurna (ex.: 1.50):", dayField,
                "Extra nocturna:", nightField,
                "Dia de descanso/feriado:", restField,
                "Base legal:", basisField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Acréscimos de Hora Extra",
                "fas-percentage",
                "Multiplicadores sobre o valor/hora normal. Confirme-os com o contabilista da "
                        + "empresa — não são valores que o sistema possa decidir.", form).showDialog();
        if (!confirmed) return;

        try {
            var request = new SaveOvertimeRateConfigRequest(
                    nameField.getText().trim(), fromField.value(), null,
                    new BigDecimal(dayField.getText().trim()),
                    new BigDecimal(nightField.getText().trim()),
                    new BigDecimal(restField.getText().trim()),
                    basisField.getText().trim().isEmpty() ? null : basisField.getText().trim());
            UIHelper.runWithProgress(owner, "A gravar acréscimos…",
                    () -> owner.hrApiClient.saveOvertimeRates(request),
                    ignored -> owner.showSuccess("Acréscimos gravados; os recibos passam a valorizar horas extra por eles."),
                    owner::showActionError);
        } catch (NumberFormatException ex) {
            owner.showNotice(FeedbackType.ERROR, "Multiplicadores inválidos", "Introduza números, por exemplo 1.50.");
        }
    }
}

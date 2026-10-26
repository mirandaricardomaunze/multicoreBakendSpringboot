package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.HrPolicyConfigDTO;
import mz.multicore.erp.modules.hr.dto.SaveHrPolicyConfigRequest;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

/** Diálogo modal para configuração dos valores legais e prazos fiscais de RH. */
final class HrPolicyConfigDialog {

    private HrPolicyConfigDialog() {}

    static void show(HRPanel owner, Runnable onSaved) {
        List<HrPolicyConfigDTO> existing;
        try {
            existing = owner.hrApiClient.getHrPolicies();
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
            return;
        }
        HrPolicyConfigDTO current = existing.stream().filter(HrPolicyConfigDTO::active)
                .findFirst().orElse(null);

        JTextField nameField = new JTextField(current == null ? "Valores legais" : current.name());
        UIHelper.styleTextField(nameField);
        DateField fromField = new DateField(LocalDate.now().withDayOfYear(1));
        JSpinner vac1 = optionalSpinner(current == null ? null : current.vacationDaysYear1(), 0, 365);
        JSpinner vac2 = optionalSpinner(current == null ? null : current.vacationDaysYear2(), 0, 365);
        JSpinner vac3 = optionalSpinner(current == null ? null : current.vacationDaysYear3Plus(), 0, 365);
        JSpinner irpsDay = optionalSpinner(current == null ? null : current.irpsDeliveryDay(), 0, 31);
        JSpinner inssDay = optionalSpinner(current == null ? null : current.inssDeliveryDay(), 0, 31);
        JSpinner noticeEmp = optionalSpinner(current == null ? null : current.noticeDaysEmployee(), 0, 365);
        JSpinner noticeEmpr = optionalSpinner(current == null ? null : current.noticeDaysEmployer(), 0, 365);
        JTextField basisField = new JTextField(current == null ? "" : orEmpty(current.legalBasis()));
        UIHelper.styleTextField(basisField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Vigora a partir de:", fromField,
                "Férias — 1.º ano (0 = por confirmar):", vac1,
                "Férias — 2.º ano:", vac2,
                "Férias — 3.º ano em diante:", vac3,
                "IRPS: dia de entrega do mês seguinte:", irpsDay,
                "INSS: dia de entrega do mês seguinte:", inssDay,
                "Aviso prévio — trabalhador (dias):", noticeEmp,
                "Aviso prévio — empregador (dias):", noticeEmpr,
                "Base legal:", basisField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Valores Legais de RH",
                "fas-balance-scale",
                "Confirme estes valores com o contabilista. O que ficar a zero é tratado como "
                        + "não configurado — e o sistema di-lo em vez de adivinhar.", form).showDialog();
        if (!confirmed) {
            return;
        }

        SaveHrPolicyConfigRequest request = new SaveHrPolicyConfigRequest(
                nameField.getText().trim(), fromField.value(), null,
                optionalValue(vac1), optionalValue(vac2), optionalValue(vac3),
                optionalValue(irpsDay), optionalValue(inssDay),
                optionalValue(noticeEmp), optionalValue(noticeEmpr),
                basisField.getText().trim().isEmpty() ? null : basisField.getText().trim());
        UIHelper.runWithProgress(owner, "A gravar valores legais…",
                () -> owner.hrApiClient.createHrPolicy(request),
                ignored -> {
                    owner.showSuccess("Valores legais gravados; as retenções passam a ter prazo a partir do próximo apuramento.");
                    onSaved.run();
                }, owner::showActionError);
    }

    private static JSpinner optionalSpinner(Integer value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value == null ? 0 : value, min, max, 1));
    }

    private static Integer optionalValue(JSpinner spinner) {
        int value = (Integer) spinner.getValue();
        return value <= 0 ? null : value;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}

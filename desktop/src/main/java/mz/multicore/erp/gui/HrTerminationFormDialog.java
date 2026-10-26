package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.CreateTerminationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.TerminationDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo modal para registo de cessação contratual e conferência prévia do acerto final.
 */
public final class HrTerminationFormDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] REASONS = {
            "INICIATIVA_TRABALHADOR", "INICIATIVA_EMPREGADOR", "MUTUO_ACORDO", "FIM_DO_TERMO", "JUSTA_CAUSA"};
    private static final String[] REASON_LABELS = {
            "Iniciativa do trabalhador", "Iniciativa do empregador", "Mútuo acordo", "Fim do termo", "Justa causa"};

    private HrTerminationFormDialog() {}

    public static void openTerminationDialog(HRPanel owner, Runnable onSuccess) {
        if (owner.employeesList.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem colaboradores", "Registe colaboradores primeiro.");
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());
        JComboBox<String> reasonCombo = new JComboBox<>(REASON_LABELS);
        UIHelper.styleComboBox(reasonCombo);
        DateField exitField = new DateField(LocalDate.now());
        JCheckBox noticeBox = new JCheckBox("Aviso prévio cumprido", true);
        noticeBox.setOpaque(false);
        noticeBox.setForeground(UIHelper.TEXT_LIGHT);
        MoneyField compensationField = new MoneyField("0.00");
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data de saída:", exitField,
                "Motivo:", reasonCombo,
                "Aviso prévio:", noticeBox,
                "Compensação (0 = nenhuma):", compensationField,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Cessar Vínculo",
                "fas-user-slash",
                "A compensação por cessação é introduzida por si: a fórmula é legal, varia com o "
                        + "motivo e a antiguidade, e não é o sistema que a decide.", form).showDialog();
        if (!confirmed) return;

        EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
        CreateTerminationRequest request = new CreateTerminationRequest(
                employee.id(), exitField.value(), REASONS[reasonCombo.getSelectedIndex()],
                noticeBox.isSelected(),
                compensationField.value().signum() > 0 ? compensationField.value() : null,
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());

        UIHelper.runWithProgress(owner, "A apurar o acerto…",
                () -> owner.hrApiClient.previewTermination(request),
                preview -> confirmAndTerminate(owner, request, preview, onSuccess),
                owner::showActionError);
    }

    private static void confirmAndTerminate(HRPanel owner, CreateTerminationRequest request,
                                            TerminationDTO preview, Runnable onSuccess) {
        StringBuilder detail = new StringBuilder();
        detail.append("<html><b>").append(preview.employeeName()).append("</b> — saída a ")
                .append(preview.terminationDate().format(DATE_FMT)).append("<br><br>");
        for (TerminationDTO.TerminationLineDTO line : preview.lines()) {
            detail.append(line.earning() ? "+ " : "− ").append(line.description()).append(" — ")
                    .append(String.format("%,.2f MT", line.amount())).append("<br>");
        }
        detail.append("<br><b>").append(preview.netAmount().signum() < 0
                        ? "Saldo a favor da empresa: " : "Líquido a receber: ")
                .append(String.format("%,.2f MT", preview.netAmount().abs())).append("</b>");
        if (!preview.warnings().isEmpty()) {
            detail.append("<br><br><b>Atenção:</b><br>");
            for (String warning : preview.warnings()) detail.append("• ").append(warning).append("<br>");
        }
        detail.append("<br><br><font color='#EF4444'><b>Aviso:</b> A cessação contratual é irreversível.</font></html>");

        JPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel detailLabel = new JLabel(detail.toString());
        detailLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        detailLabel.setForeground(UIHelper.TEXT_LIGHT);
        card.add(detailLabel, BorderLayout.CENTER);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Acerto Final & Cessação",
                "fas-user-slash", "Conferência prévia dos proporcionais apurados", card)
                .setConfirmButton("Confirmar Cessação", "fas-check")
                .showDialog();
        if (!confirmed) return;

        UIHelper.runWithProgress(owner, "A cessar o vínculo…",
                () -> owner.hrApiClient.terminate(request),
                ignored -> {
                    owner.showSuccess("Cessação registada; o acerto fica por pagar até ser liquidado.");
                    if (onSuccess != null) onSuccess.run();
                    owner.refreshData();
                }, owner::showActionError);
    }
}

package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Diálogo modal para passagem de turno entre operadores. Permite contagem
 * cega da gaveta e selecção do operador seguinte, registando reconciliação
 * parcial sem fechar a sessão.
 */
public class PosShiftHandoverDialog extends JDialog {

    private final POSApiClient posApiClient;
    private final TillSessionDTO session;
    private final Runnable onSuccess;

    private final PosCashBreakdownPanel breakdownPanel = new PosCashBreakdownPanel();
    private final PosShiftHandoverResultPanel resultPanel = new PosShiftHandoverResultPanel();

    private JTextField incomingOperatorField;
    private JTextArea notesArea;
    private ModernButton btnConfirm;
    private ModernButton btnCancel;

    public PosShiftHandoverDialog(Window owner, POSApiClient posApiClient,
                                   TillSessionDTO session, Runnable onSuccess) {
        super(owner, "Passagem de Turno — Sessão #" + session.id(), ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.session = session;
        this.onSuccess = onSuccess;

        setSize(720, 760);
        setMinimumSize(new Dimension(680, 680));
        setLocationRelativeTo(owner);
        String effectiveOp = session.currentOperator() != null ? session.currentOperator() : session.operator();
        setContentPane(DialogChrome.frame("fas-people-arrows", "Passagem de Turno",
                "Operador actual: " + (effectiveOp != null ? effectiveOp : "—") + " | Sessão #" + session.id(),
                createContentPanel(), createFooterPanel()));

        breakdownPanel.recalculate();
    }

    private JComponent createContentPanel() {
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        content.add(createOperatorPanel());
        content.add(Box.createVerticalStrut(8));
        breakdownPanel.addTo(content);
        content.add(Box.createVerticalStrut(10));
        content.add(createNotesPanel());
        content.add(Box.createVerticalStrut(10));
        content.add(resultPanel.component());

        JScrollPane scroll = new JScrollPane(content);
        UIHelper.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        return scroll;
    }

    private JPanel createOperatorPanel() {
        JPanel panel = new ModernPanel(12); panel.setLayout(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(Spacing.MD, Spacing.LG, Spacing.MD, Spacing.LG));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblOp = new JLabel("Operador Seguinte (username):");
        lblOp.setFont(Typography.EMPHASIS_BOLD); lblOp.setForeground(Color.WHITE);

        incomingOperatorField = new JTextField(); UIHelper.styleTextField(incomingOperatorField);
        incomingOperatorField.setFont(Typography.EMPHASIS);
        incomingOperatorField.putClientProperty("JTextField.placeholderText", "Ex.: operador2");

        panel.add(lblOp, BorderLayout.NORTH); panel.add(incomingOperatorField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNotesPanel() {
        JPanel panel = new ModernPanel(12); panel.setLayout(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(Spacing.SM, Spacing.LG, Spacing.SM, Spacing.LG));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel lbl = new JLabel("Observações / Justificação (opcional):");
        lbl.setFont(Typography.BODY_BOLD); lbl.setForeground(UIHelper.TEXT_LIGHT);

        notesArea = new JTextArea(2, 20); notesArea.setFont(Typography.BODY);
        notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
        notesArea.setBackground(UIHelper.ROW_ALT); notesArea.setForeground(Color.WHITE);
        notesArea.setCaretColor(Color.WHITE); notesArea.setBorder(BorderFactory.createEmptyBorder(Spacing.XS, Spacing.XS, Spacing.XS, Spacing.XS));

        panel.add(lbl, BorderLayout.NORTH); panel.add(notesArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooterPanel() {
        btnConfirm = ButtonStyles.main(UIHelper.createPrimaryButton("Confirmar Passagem"));
        btnConfirm.setIcon(UIHelper.icon("fas-check", 13));
        btnConfirm.addActionListener(e -> performHandover());

        btnCancel = ButtonStyles.cancelButton();
        btnCancel.setIcon(UIHelper.icon("fas-times", 13));
        btnCancel.addActionListener(e -> dispose());

        return DialogChrome.footer(btnCancel, btnConfirm);
    }

    private void performHandover() {
        String incomingOperator = incomingOperatorField.getText().trim();
        if (incomingOperator.isBlank()) {
            ToastManager.show(this, FeedbackType.WARNING, "Indique o username do operador seguinte.");
            incomingOperatorField.requestFocusInWindow();
            return;
        }

        BigDecimal countedCash = breakdownPanel.calculateCountedAmount();
        if (countedCash == null || countedCash.compareTo(BigDecimal.ZERO) < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Introduza o valor total contado na gaveta.");
            return;
        }

        String effectiveOperator = session.currentOperator() != null ? session.currentOperator() : session.operator();
        String notes = notesArea.getText().trim();

        ShiftHandoverRequest request = new ShiftHandoverRequest(
                session.id(), effectiveOperator, incomingOperator, countedCash,
                breakdownPanel.buildDenominationsJson(), notes.isEmpty() ? null : notes
        );

        btnConfirm.setEnabled(false); btnConfirm.setText("A processar...");
        UIHelper.runWithProgress(this, "A registar passagem de turno",
                () -> posApiClient.performShiftHandover(session.id(), request),
                this::onHandoverSuccess,
                error -> {
                    btnConfirm.setEnabled(true); btnConfirm.setText("Confirmar Passagem");
                    ToastManager.show(this, FeedbackType.ERROR, "Erro na passagem de turno: " + error.getMessage());
                });
    }

    private void onHandoverSuccess(ShiftReconciliationDTO result) {
        btnConfirm.setVisible(false); btnCancel.setText("Fechar");
        breakdownPanel.setControlsEnabled(false);
        incomingOperatorField.setEnabled(false);
        notesArea.setEnabled(false);

        resultPanel.showResult(result);
        ToastManager.success(this, "Passagem de turno concluída: " + result.outgoingOperator() + " -> " + result.incomingOperator());
        if (onSuccess != null) onSuccess.run();
    }
}

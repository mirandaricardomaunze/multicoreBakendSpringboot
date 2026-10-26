package mz.multicore.erp.gui.pos;

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
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        breakdownPanel.recalculate();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new ModernPanel(16);
        header.setLayout(new BorderLayout(12, 8)); header.setBackground(UIHelper.SELECTION_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel icon = new JLabel(UIHelper.icon("fas-people-arrows", 28, UIHelper.ACCENT_CYAN));
        JLabel title = new JLabel("Passagem de Turno");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18)); title.setForeground(Color.WHITE);

        String effectiveOp = session.currentOperator() != null ? session.currentOperator() : session.operator();
        JLabel subtitle = new JLabel("Operador actual: " + (effectiveOp != null ? effectiveOp : "—") + " | Sessão #" + session.id());
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); subtitle.setForeground(UIHelper.TEXT_LIGHT);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2)); textPanel.setOpaque(false);
        textPanel.add(title); textPanel.add(subtitle);

        header.add(icon, BorderLayout.WEST); header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JComponent createContentPanel() {
        JPanel content = new JPanel(); content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIHelper.BG_DARK); content.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

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
        return scroll;
    }

    private JPanel createOperatorPanel() {
        JPanel panel = new ModernPanel(12); panel.setLayout(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblOp = new JLabel("Operador Seguinte (username):");
        lblOp.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); lblOp.setForeground(Color.WHITE);

        incomingOperatorField = new JTextField(); UIHelper.styleTextField(incomingOperatorField);
        incomingOperatorField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 14));
        incomingOperatorField.putClientProperty("JTextField.placeholderText", "Ex.: operador2");

        panel.add(lblOp, BorderLayout.NORTH); panel.add(incomingOperatorField, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNotesPanel() {
        JPanel panel = new ModernPanel(12); panel.setLayout(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT); panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        JLabel lbl = new JLabel("Observações / Justificação (opcional):");
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); lbl.setForeground(UIHelper.TEXT_LIGHT);

        notesArea = new JTextArea(2, 20); notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesArea.setLineWrap(true); notesArea.setWrapStyleWord(true);
        notesArea.setBackground(UIHelper.ROW_ALT); notesArea.setForeground(Color.WHITE);
        notesArea.setCaretColor(Color.WHITE); notesArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        panel.add(lbl, BorderLayout.NORTH); panel.add(notesArea, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12)); footer.setBackground(UIHelper.ROW_ALT);
        btnConfirm = UIHelper.createPrimaryButton("Confirmar Passagem");
        btnConfirm.setIcon(UIHelper.icon("fas-check", 13, Color.WHITE));
        btnConfirm.addActionListener(e -> performHandover());

        btnCancel = UIHelper.createSecondaryButton("Cancelar");
        btnCancel.setIcon(UIHelper.icon("fas-times", 13));
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnConfirm); footer.add(btnCancel);
        return footer;
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

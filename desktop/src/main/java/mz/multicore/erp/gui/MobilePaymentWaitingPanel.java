package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Painel visual de espera por autorização PIN (Push USSD) com contagem regressiva e progresso.
 */
public final class MobilePaymentWaitingPanel extends ModernPanel {

    private final JLabel statusDescLabel;
    private final JLabel countdownLabel;
    private final JProgressBar progressBar;
    private final ModernButton simulateApproveBtn;
    private final ModernButton cancelButton;

    public MobilePaymentWaitingPanel(Runnable onCancel, Runnable onSimulate) {
        super(10);
        setBackground(UIHelper.ROW_ALT);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setVisible(false);

        JPanel waitHeader = new JPanel(new BorderLayout());
        waitHeader.setOpaque(false);
        statusDescLabel = new JLabel("Aguardando PIN do cliente...");
        statusDescLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        statusDescLabel.setForeground(UIHelper.PENDING_YELLOW);

        countdownLabel = new JLabel("60s");
        countdownLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        countdownLabel.setForeground(UIHelper.TEXT_MUTED);

        waitHeader.add(statusDescLabel, BorderLayout.WEST);
        waitHeader.add(countdownLabel, BorderLayout.EAST);
        add(waitHeader);
        add(Box.createRigidArea(new Dimension(0, 8)));

        progressBar = new JProgressBar(0, 60);
        progressBar.setValue(60);
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setForeground(UIHelper.ACCENT_BLUE);
        add(progressBar);
        add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel waitActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        waitActions.setOpaque(false);

        simulateApproveBtn = new ModernButton("Simular Aprovação", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        simulateApproveBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        simulateApproveBtn.setPreferredSize(new Dimension(140, 28));
        simulateApproveBtn.addActionListener(e -> { if (onSimulate != null) onSimulate.run(); });

        cancelButton = ButtonStyles.cancelButton();
        cancelButton.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        cancelButton.setPreferredSize(new Dimension(90, 28));
        cancelButton.addActionListener(e -> { if (onCancel != null) onCancel.run(); });

        waitActions.add(simulateApproveBtn);
        waitActions.add(cancelButton);
        add(waitActions);
    }

    public void startWaiting(String initialStatus) {
        setVisible(true);
        statusDescLabel.setText(initialStatus);
        statusDescLabel.setForeground(UIHelper.PENDING_YELLOW);
        countdownLabel.setIcon(null);
        countdownLabel.setText("60s");
        progressBar.setValue(60);
        progressBar.setForeground(UIHelper.ACCENT_BLUE);
    }

    public void updateCountdown(int remainingSeconds) {
        countdownLabel.setText(remainingSeconds + "s");
        progressBar.setValue(remainingSeconds);
    }

    public void setStatusMessage(String message, Color color) {
        statusDescLabel.setText(message);
        statusDescLabel.setForeground(color);
    }

    public void showSuccess(String financialRef) {
        statusDescLabel.setText("Aprovado com sucesso! (" + financialRef + ")");
        statusDescLabel.setForeground(UIHelper.APPROVED_GREEN);
        progressBar.setForeground(UIHelper.APPROVED_GREEN);
        countdownLabel.setIcon(UIHelper.icon("fas-check", 16, UIHelper.APPROVED_GREEN));
        countdownLabel.setText("");
    }

    public void showFailed(String reason) {
        statusDescLabel.setText("Pagamento recusado: " + (reason != null ? reason : "Erro no PIN"));
        statusDescLabel.setForeground(UIHelper.REJECTED_RED);
    }

    public void showTimeout() {
        statusDescLabel.setText("Tempo limite excedido (60s). Tente novamente.");
        statusDescLabel.setForeground(UIHelper.REJECTED_RED);
    }
}

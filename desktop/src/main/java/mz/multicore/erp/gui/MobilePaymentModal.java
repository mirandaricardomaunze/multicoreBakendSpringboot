package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Diálogo executivo para pagamento móvel Push USSD (M-Pesa e e-Mola).
 * Envia pedido de débito ao telemóvel do cliente, exibe contagem regressiva
 * e aguarda autorização via PIN com feedback sonoro e visual em tempo real.
 */
public final class MobilePaymentModal extends JDialog implements MobilePaymentWorkflowCoordinator.Listener {

    private final BigDecimal amount;
    private String approvedReference = null;

    private final MobilePaymentInitiationPanel initiationPanel;
    private final MobilePaymentWaitingPanel waitingPanel;
    private final MobilePaymentWorkflowCoordinator coordinator;

    public static String show(Component parent, MobilePaymentProvider initialProvider,
                              BigDecimal totalAmount, POSApiClient client) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        MobilePaymentModal modal = new MobilePaymentModal(owner, initialProvider, totalAmount, client);
        modal.setVisible(true);
        return modal.approvedReference;
    }

    private MobilePaymentModal(Window owner, MobilePaymentProvider initialProvider,
                               BigDecimal totalAmount, POSApiClient client) {
        super(owner, "Pagamento Móvel Push USSD", ModalityType.APPLICATION_MODAL);
        this.amount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.coordinator = new MobilePaymentWorkflowCoordinator(client, this);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(460, 480);
        setLocationRelativeTo(owner);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UIHelper.BG_DARK);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        content.add(buildHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        initiationPanel = new MobilePaymentInitiationPanel(this.amount, initialProvider, this::startPaymentFlow);
        waitingPanel = new MobilePaymentWaitingPanel(coordinator::cancelActiveTransaction, () -> coordinator.forceSimulateComplete(true));

        centerPanel.add(initiationPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(waitingPanel);
        content.add(centerPanel, BorderLayout.CENTER);
        setContentPane(content);
    }

    private JPanel buildHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));
        headerPanel.setOpaque(false);
        headerPanel.add(new JLabel(UIHelper.icon("fas-mobile-alt", 26, UIHelper.ACCENT_BLUE)), BorderLayout.WEST);

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 2));
        headerText.setOpaque(false);
        JLabel titleLabel = new JLabel("Pagamento por Telemóvel (Push USSD)");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);
        JLabel subtitleLabel = new JLabel("O cliente receberá uma solicitação no visor para digitar o PIN.");
        subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);

        headerText.add(titleLabel); headerText.add(subtitleLabel);
        headerPanel.add(headerText, BorderLayout.CENTER);
        return headerPanel;
    }

    private void startPaymentFlow() {
        initiationPanel.setControlsEnabled(false);
        coordinator.startPayment(initiationPanel.getSelectedProvider(), initiationPanel.getPhoneNumber(), this.amount);
    }

    @Override public void onWaitingStarted(String message) { waitingPanel.startWaiting(message); }
    @Override public void onCountdownTick(int remainingSeconds) { waitingPanel.updateCountdown(remainingSeconds); }

    @Override
    public void onSuccess(String financialRef) {
        this.approvedReference = financialRef; waitingPanel.showSuccess(financialRef);
        Timer closeTimer = new Timer(800, e -> dispose()); closeTimer.setRepeats(false); closeTimer.start();
    }

    @Override public void onFailed(String message) { waitingPanel.showFailed(message); initiationPanel.setControlsEnabled(true); }
    @Override public void onTimeout() { waitingPanel.showTimeout(); initiationPanel.setControlsEnabled(true); }

    @Override
    public void onError(String error) {
        waitingPanel.setVisible(false); initiationPanel.setControlsEnabled(true);
        ToastManager.show(this, FeedbackType.ERROR, "Falha ao iniciar pagamento móvel: " + error);
    }

    @Override public void onCancelled() { waitingPanel.setVisible(false); initiationPanel.setControlsEnabled(true); }
    @Override public void dispose() { coordinator.stopTimers(); super.dispose(); }
}

package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine;
import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.pos.dto.MobilePaymentResponse;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatus;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Diálogo executivo para pagamento móvel Push USSD (M-Pesa e e-Mola).
 * Envia pedido de débito ao telemóvel do cliente, exibe contagem regressiva
 * e aguarda autorização via PIN com feedback sonoro e visual em tempo real.
 */
public final class MobilePaymentModal extends JDialog {

    private final POSApiClient apiClient;
    private final BigDecimal amount;
    private MobilePaymentProvider currentProvider;
    private String approvedReference = null;

    private final JComboBox<MobilePaymentProvider> providerCombo;
    private final JTextField phoneField;
    private final JLabel validationLabel;
    private final ModernButton sendButton;
    private final JPanel waitingPanel;
    private final JLabel countdownLabel;
    private final JProgressBar progressBar;
    private final JLabel statusDescLabel;
    private final ModernButton cancelButton;
    private final ModernButton simulateApproveBtn;

    private Timer countdownTimer;
    private Timer pollTimer;
    private String activeTransactionId;
    private int remainingSeconds = 60;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

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
        this.apiClient = client;
        this.amount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        this.currentProvider = initialProvider != null ? initialProvider : MobilePaymentProvider.MPESA;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(460, 480);
        setLocationRelativeTo(owner);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UIHelper.BG_DARK);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Cabeçalho
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UIHelper.icon("fas-mobile-alt", 26, UIHelper.ACCENT_BLUE));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 2));
        headerText.setOpaque(false);
        JLabel titleLabel = new JLabel("Pagamento por Telemóvel (Push USSD)");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("O cliente receberá uma solicitação no visor para digitar o PIN.");
        subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);

        headerText.add(titleLabel);
        headerText.add(subtitleLabel);
        headerPanel.add(headerText, BorderLayout.CENTER);
        content.add(headerPanel, BorderLayout.NORTH);

        // Corpo central
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Card do Valor
        ModernPanel amountCard = new ModernPanel(10);
        amountCard.setBackground(UIHelper.ROW_ALT);
        amountCard.setBorder(new EmptyBorder(10, 16, 10, 16));
        amountCard.setLayout(new BorderLayout(8, 2));

        JLabel amountTitle = new JLabel("TOTAL A COBRAR:");
        amountTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        amountTitle.setForeground(UIHelper.TEXT_MUTED);

        JLabel amountVal = new JLabel(String.format("%,.2f MT", this.amount));
        amountVal.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        amountVal.setForeground(UIHelper.ACCENT_BLUE);
        amountVal.setHorizontalAlignment(SwingConstants.RIGHT);

        amountCard.add(amountTitle, BorderLayout.WEST);
        amountCard.add(amountVal, BorderLayout.EAST);
        amountCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(amountCard);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Seletor de Operador
        JLabel providerLabel = new JLabel("Operador de Pagamento:");
        providerLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        providerLabel.setForeground(UIHelper.TEXT_LIGHT);
        providerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(providerLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        providerCombo = new JComboBox<>(MobilePaymentProvider.values());
        providerCombo.setSelectedItem(this.currentProvider);
        UIHelper.styleComboBox(providerCombo);
        providerCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        providerCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        providerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MobilePaymentProvider p) {
                    setText(p.getDisplayName() + " (" + p.getOperator() + " — " + p.getPrefixes() + ")");
                }
                return this;
            }
        });
        centerPanel.add(providerCombo);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Campo de Telefone
        JLabel phoneLabel = new JLabel("Número de Telemóvel:");
        phoneLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        phoneLabel.setForeground(UIHelper.TEXT_LIGHT);
        phoneLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(phoneLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        phoneField = new JTextField();
        UIHelper.styleTextField(phoneField);
        phoneField.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        phoneField.putClientProperty("JTextField.placeholderText", "Ex.: 84 123 4567 ou 86 123 4567");
        phoneField.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        centerPanel.add(phoneField);

        validationLabel = new JLabel("Introduza o número de 9 dígitos");
        validationLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        validationLabel.setForeground(UIHelper.TEXT_MUTED);
        validationLabel.setBorder(new EmptyBorder(3, 2, 8, 2));
        validationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(validationLabel);

        // Botão Enviar Pedido USSD
        sendButton = UIHelper.createPrimaryButton("Enviar Pedido ao Telemóvel (Push USSD)");
        sendButton.setIcon(UIHelper.icon("fas-paper-plane", 13, Color.WHITE));
        sendButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        sendButton.setPreferredSize(new Dimension(0, 40));
        sendButton.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        sendButton.setEnabled(false);
        centerPanel.add(sendButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Painel de Espera e Polling
        waitingPanel = new ModernPanel(10);
        waitingPanel.setBackground(UIHelper.ROW_ALT);
        waitingPanel.setBorder(new EmptyBorder(12, 14, 12, 14));
        waitingPanel.setLayout(new BoxLayout(waitingPanel, BoxLayout.Y_AXIS));
        waitingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        waitingPanel.setVisible(false);

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
        waitingPanel.add(waitHeader);
        waitingPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        progressBar = new JProgressBar(0, 60);
        progressBar.setValue(60);
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setForeground(UIHelper.ACCENT_BLUE);
        waitingPanel.add(progressBar);
        waitingPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel waitActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        waitActions.setOpaque(false);

        simulateApproveBtn = new ModernButton("Simular Aprovação", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        simulateApproveBtn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        simulateApproveBtn.setPreferredSize(new Dimension(140, 28));
        simulateApproveBtn.addActionListener(e -> forceSimulateComplete(true));

        cancelButton = UIHelper.createDangerButton("Cancelar");
        cancelButton.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        cancelButton.setPreferredSize(new Dimension(90, 28));
        cancelButton.addActionListener(e -> cancelActiveTransaction());

        waitActions.add(simulateApproveBtn);
        waitActions.add(cancelButton);
        waitingPanel.add(waitActions);

        centerPanel.add(waitingPanel);
        content.add(centerPanel, BorderLayout.CENTER);

        // Lógica de validação em tempo real
        Runnable validateInput = () -> {
            MobilePaymentProvider prov = (MobilePaymentProvider) providerCombo.getSelectedItem();
            String raw = phoneField.getText();
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.startsWith("258") && digits.length() == 12) {
                digits = digits.substring(3);
            }

            if (digits.isEmpty()) {
                validationLabel.setText("Introduza o número de 9 dígitos");
                validationLabel.setForeground(UIHelper.TEXT_MUTED);
                sendButton.setEnabled(false);
                return;
            }

            if (digits.length() == 9) {
                String prefix = digits.substring(0, 2);
                boolean validForProvider = false;
                if (prov == MobilePaymentProvider.MPESA && (prefix.equals("84") || prefix.equals("85"))) {
                    validForProvider = true;
                } else if (prov == MobilePaymentProvider.EMOLA && (prefix.equals("86") || prefix.equals("87"))) {
                    validForProvider = true;
                }

                if (validForProvider) {
                    validationLabel.setIcon(UIHelper.icon("fas-check-circle", 13, UIHelper.APPROVED_GREEN));
                    validationLabel.setText("Número " + prov.getOperator() + " válido (" + digits + ")");
                    validationLabel.setForeground(UIHelper.APPROVED_GREEN);
                    sendButton.setEnabled(!isProcessing.get());
                } else {
                    validationLabel.setIcon(UIHelper.icon("fas-exclamation-triangle", 13, UIHelper.REJECTED_RED));
                    validationLabel.setText("Prefixo " + prefix + " incompatível com " + prov.getDisplayName()
                            + " (" + prov.getPrefixes() + ")");
                    validationLabel.setForeground(UIHelper.REJECTED_RED);
                    sendButton.setEnabled(false);
                }
            } else {
                validationLabel.setIcon(null);
                validationLabel.setText("Faltam " + (9 - digits.length()) + " dígitos (" + digits.length() + "/9)");
                validationLabel.setForeground(UIHelper.TEXT_MUTED);
                sendButton.setEnabled(false);
            }
        };

        providerCombo.addActionListener(e -> validateInput.run());
        UIHelper.onTextChange(phoneField, validateInput);

        sendButton.addActionListener(e -> startPaymentFlow());

        setContentPane(content);
    }

    private void startPaymentFlow() {
        if (isProcessing.get()) return;
        isProcessing.set(true);

        sendButton.setEnabled(false);
        providerCombo.setEnabled(false);
        phoneField.setEnabled(false);

        MobilePaymentProvider provider = (MobilePaymentProvider) providerCombo.getSelectedItem();
        String phone = phoneField.getText().trim();
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null
                ? CurrentUserContext.findCurrentCompanyId() : 1L;
        String operator = CurrentUserContext.getUsername();

        waitingPanel.setVisible(true);
        remainingSeconds = 60;
        progressBar.setValue(60);
        countdownLabel.setText("60s");
        statusDescLabel.setText("A enviar pedido USSD ao telemóvel...");
        statusDescLabel.setForeground(UIHelper.PENDING_YELLOW);

        SwingWorker<MobilePaymentResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected MobilePaymentResponse doInBackground() throws Exception {
                InitiateMobilePaymentRequest req = new InitiateMobilePaymentRequest(
                        provider, phone, amount, "POS-" + System.currentTimeMillis(), companyId, operator
                );
                return apiClient.initiateMobilePayment(req);
            }

            @Override
            protected void done() {
                try {
                    MobilePaymentResponse resp = get();
                    activeTransactionId = resp.transactionId();
                    statusDescLabel.setText("Aguardando PIN no telemóvel do cliente...");
                    startTimers();
                } catch (Exception ex) {
                    isProcessing.set(false);
                    waitingPanel.setVisible(false);
                    sendButton.setEnabled(true);
                    providerCombo.setEnabled(true);
                    phoneField.setEnabled(true);
                    PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
                    JOptionPane.showMessageDialog(MobilePaymentModal.this,
                            "Falha ao iniciar pagamento móvel: " + ex.getMessage(),
                            "Erro de Comunicação", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void startTimers() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            countdownLabel.setText(remainingSeconds + "s");
            progressBar.setValue(remainingSeconds);
            if (remainingSeconds <= 0) {
                stopTimers();
                handleTimeout();
            }
        });
        countdownTimer.start();

        // Polling a cada 2 segundos
        pollTimer = new Timer(2000, e -> pollStatus());
        pollTimer.start();
    }

    private void pollStatus() {
        if (activeTransactionId == null) return;
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null
                ? CurrentUserContext.findCurrentCompanyId() : 1L;

        SwingWorker<MobilePaymentStatusResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected MobilePaymentStatusResponse doInBackground() {
                try {
                    return apiClient.getMobilePaymentStatus(activeTransactionId, companyId);
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    MobilePaymentStatusResponse st = get();
                    if (st != null) {
                        if (st.status() == MobilePaymentStatus.SUCCESS) {
                            stopTimers();
                            handleSuccess(st.financialReference());
                        } else if (st.status() == MobilePaymentStatus.FAILED || st.status() == MobilePaymentStatus.CANCELLED) {
                            stopTimers();
                            handleFailed(st.message());
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void handleSuccess(String financialRef) {
        this.approvedReference = financialRef;
        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS);
        statusDescLabel.setText("Aprovado com sucesso! (" + financialRef + ")");
        statusDescLabel.setForeground(UIHelper.APPROVED_GREEN);
        progressBar.setForeground(UIHelper.APPROVED_GREEN);
        countdownLabel.setIcon(UIHelper.icon("fas-check", 16, UIHelper.APPROVED_GREEN));
        countdownLabel.setText("");

        Timer closeTimer = new Timer(800, e -> dispose());
        closeTimer.setRepeats(false);
        closeTimer.start();
    }

    private void handleFailed(String msg) {
        isProcessing.set(false);
        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
        statusDescLabel.setText("Pagamento recusado: " + (msg != null ? msg : "Erro no PIN"));
        statusDescLabel.setForeground(UIHelper.REJECTED_RED);
        sendButton.setEnabled(true);
        providerCombo.setEnabled(true);
        phoneField.setEnabled(true);
    }

    private void handleTimeout() {
        isProcessing.set(false);
        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
        statusDescLabel.setText("Tempo limite excedido (60s). Tente novamente.");
        statusDescLabel.setForeground(UIHelper.REJECTED_RED);
        sendButton.setEnabled(true);
        providerCombo.setEnabled(true);
        phoneField.setEnabled(true);
    }

    private void forceSimulateComplete(boolean approve) {
        if (activeTransactionId == null) return;
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null
                ? CurrentUserContext.findCurrentCompanyId() : 1L;

        SwingWorker<MobilePaymentStatusResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected MobilePaymentStatusResponse doInBackground() throws Exception {
                return apiClient.simulateCompleteMobilePayment(activeTransactionId, companyId, approve);
            }

            @Override
            protected void done() {
                try {
                    MobilePaymentStatusResponse st = get();
                    if (st != null && st.status() == MobilePaymentStatus.SUCCESS) {
                        stopTimers();
                        handleSuccess(st.financialReference());
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void cancelActiveTransaction() {
        stopTimers();
        isProcessing.set(false);
        waitingPanel.setVisible(false);
        sendButton.setEnabled(true);
        providerCombo.setEnabled(true);
        phoneField.setEnabled(true);
    }

    private void stopTimers() {
        if (countdownTimer != null) countdownTimer.stop();
        if (pollTimer != null) pollTimer.stop();
    }

    @Override
    public void dispose() {
        stopTimers();
        super.dispose();
    }
}

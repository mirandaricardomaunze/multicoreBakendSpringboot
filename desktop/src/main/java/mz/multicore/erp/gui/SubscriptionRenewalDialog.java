package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentResultDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

/**
 * Diálogo executivo para ativação e renovação de planos da plataforma pelo assinante.
 * Suporta pagamento móvel automático por Push USSD (M-Pesa / e-Mola) e transferência manual.
 */
public final class SubscriptionRenewalDialog extends JDialog {

    private final MySubscriptionApiClient subscriptionClient;
    private final POSApiClient posApiClient;
    private final Consumer<MySubscriptionDTO> onSuccess;

    private final JComboBox<String> planCombo;
    private final JComboBox<String> periodCombo;
    private final JLabel totalLabel;
    private final JLabel discountBadge;

    private final JRadioButton mpesaRadio;
    private final JRadioButton emolaRadio;
    private final JRadioButton bankRadio;

    private final JTextField phoneField;
    private final JTextField refField;
    private final JTextArea notesArea;
    private final JPanel mobileCard;
    private final JPanel bankCard;
    private final ModernButton submitBtn;

    public static void show(Component parent, MySubscriptionApiClient subClient,
                            Consumer<MySubscriptionDTO> onSuccess) {
        show(parent, subClient, subClient.getPosApiClient(), onSuccess);
    }

    public static void show(Component parent, MySubscriptionApiClient subClient,
                            POSApiClient posClient, Consumer<MySubscriptionDTO> onSuccess) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        SubscriptionRenewalDialog dialog = new SubscriptionRenewalDialog(owner, subClient, posClient, onSuccess);
        dialog.setVisible(true);
    }

    private SubscriptionRenewalDialog(Window owner, MySubscriptionApiClient subClient,
                                      POSApiClient posClient, Consumer<MySubscriptionDTO> onSuccess) {
        super(owner, "Activação e Renovação de Plano", ModalityType.APPLICATION_MODAL);
        this.subscriptionClient = subClient;
        this.posApiClient = posClient;
        this.onSuccess = onSuccess;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(560, 640);
        setLocationRelativeTo(owner);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(UIHelper.BG_DARK);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Cabeçalho
        JPanel headerPanel = new JPanel(new BorderLayout(12, 4));
        headerPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(UIHelper.icon("fas-crown", 28, UIHelper.PENDING_YELLOW));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel headerText = new JPanel(new GridLayout(2, 1, 0, 2));
        headerText.setOpaque(false);
        JLabel titleLabel = new JLabel("Activar ou Renovar Plano do Sistema");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        titleLabel.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitleLabel = new JLabel("Seleccione o seu plano comercial e efectue o pagamento.");
        subtitleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitleLabel.setForeground(UIHelper.TEXT_MUTED);

        headerText.add(titleLabel);
        headerText.add(subtitleLabel);
        headerPanel.add(headerText, BorderLayout.CENTER);
        content.add(headerPanel, BorderLayout.NORTH);

        // Corpo
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // 1. Seleção de Plano e Período
        String[] plans = {"PRO — Profissional (3.500 MT/mês)", "BASIC — Básico (1.500 MT/mês)", "ENTERPRISE — Empresarial (7.500 MT/mês)"};
        planCombo = new JComboBox<>(plans);
        UIHelper.styleComboBox(planCombo);

        String[] periods = {"1 Mês (Sem desconto)", "3 Meses (5% de Desconto)", "6 Meses (10% de Desconto)", "12 Meses (15% Desconto Anual)"};
        periodCombo = new JComboBox<>(periods);
        UIHelper.styleComboBox(periodCombo);

        JPanel configForm = UIHelper.createDialogForm(
                "Plano comercial pretendido:", planCombo,
                "Período de subscrição:", periodCombo
        );
        configForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(configForm);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        // Card do Valor Total Calculado
        ModernPanel totalCard = new ModernPanel(12);
        totalCard.setBackground(UIHelper.ROW_ALT);
        totalCard.setBorder(new EmptyBorder(12, 16, 12, 16));
        totalCard.setLayout(new BorderLayout(8, 2));
        totalCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel totalLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        totalLeft.setOpaque(false);
        JLabel totalTitle = new JLabel("VALOR TOTAL:");
        totalTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        totalTitle.setForeground(UIHelper.TEXT_MUTED);
        discountBadge = new JLabel("");
        discountBadge.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        discountBadge.setForeground(UIHelper.APPROVED_GREEN);
        totalLeft.add(totalTitle);
        totalLeft.add(discountBadge);

        totalLabel = new JLabel("3.500,00 MT");
        totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        totalLabel.setForeground(UIHelper.ACCENT_BLUE);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        totalCard.add(totalLeft, BorderLayout.WEST);
        totalCard.add(totalLabel, BorderLayout.EAST);
        body.add(totalCard);
        body.add(Box.createRigidArea(new Dimension(0, 14)));

        // 2. Opções de Pagamento
        JLabel methodTitle = new JLabel("Forma de Pagamento:");
        methodTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        methodTitle.setForeground(UIHelper.TEXT_LIGHT);
        methodTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(methodTitle);
        body.add(Box.createRigidArea(new Dimension(0, 6)));

        ButtonGroup group = new ButtonGroup();
        mpesaRadio = new JRadioButton("M-Pesa (Push USSD)", true);
        emolaRadio = new JRadioButton("e-Mola (Push USSD)");
        bankRadio = new JRadioButton("Transferência Bancária / Comprovativo Manual");

        mpesaRadio.setOpaque(false);
        emolaRadio.setOpaque(false);
        bankRadio.setOpaque(false);
        mpesaRadio.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        emolaRadio.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        bankRadio.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));

        group.add(mpesaRadio);
        group.add(emolaRadio);
        group.add(bankRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        radioPanel.setOpaque(false);
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioPanel.add(mpesaRadio);
        radioPanel.add(emolaRadio);
        radioPanel.add(bankRadio);
        body.add(radioPanel);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        // Painel para Pagamento Móvel
        mobileCard = new ModernPanel(10);
        mobileCard.setBackground(UIHelper.ROW_ALT);
        mobileCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        mobileCard.setLayout(new BorderLayout(8, 6));
        mobileCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel phonePrompt = new JLabel("Número de Telemóvel para receber o prompt de PIN:");
        phonePrompt.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        phonePrompt.setForeground(UIHelper.TEXT_MUTED);

        phoneField = new JTextField();
        UIHelper.styleTextField(phoneField);
        phoneField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        phoneField.putClientProperty("JTextField.placeholderText", "Ex.: 84 123 4567 ou 86 123 4567");

        mobileCard.add(phonePrompt, BorderLayout.NORTH);
        mobileCard.add(phoneField, BorderLayout.CENTER);
        body.add(mobileCard);

        // Painel para Transferência Manual
        bankCard = new ModernPanel(10);
        bankCard.setBackground(UIHelper.ROW_ALT);
        bankCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        bankCard.setLayout(new BoxLayout(bankCard, BoxLayout.Y_AXIS));
        bankCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankCard.setVisible(false);

        JLabel bankPrompt = new JLabel("Coordenadas Bancárias da Plataforma: Millennium BIM: 123456789 | BCI: 987654321");
        bankPrompt.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        bankPrompt.setForeground(UIHelper.TEXT_MUTED);

        refField = new JTextField();
        UIHelper.styleTextField(refField);
        refField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        refField.putClientProperty("JTextField.placeholderText", "Nº de Comprovativo ou Código de Depósito");

        notesArea = new JTextArea(2, 20);
        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesArea.setLineWrap(true);

        bankCard.add(bankPrompt);
        bankCard.add(Box.createRigidArea(new Dimension(0, 6)));
        bankCard.add(new JLabel("Referência do Pagamento:"));
        bankCard.add(refField);
        bankCard.add(Box.createRigidArea(new Dimension(0, 6)));
        bankCard.add(new JLabel("Observações adicionais:"));
        bankCard.add(new JScrollPane(notesArea));
        body.add(bankCard);

        content.add(body, BorderLayout.CENTER);

        // Rodapé com Ações
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.addActionListener(e -> dispose());

        submitBtn = UIHelper.createPrimaryButton("Proceder ao Pagamento");
        submitBtn.setIcon(UIHelper.icon("fas-check-circle", 13, Color.WHITE));
        submitBtn.addActionListener(e -> processRenewal());

        footer.add(cancelBtn);
        footer.add(submitBtn);
        content.add(footer, BorderLayout.SOUTH);

        // Listeners reativos
        Runnable updateCalculations = this::recalculateTotal;
        planCombo.addActionListener(e -> updateCalculations.run());
        periodCombo.addActionListener(e -> updateCalculations.run());

        Runnable toggleMethod = () -> {
            boolean isBank = bankRadio.isSelected();
            mobileCard.setVisible(!isBank);
            bankCard.setVisible(isBank);
            submitBtn.setText(isBank ? "Submeter Comprovativo" : "Enviar Pedido USSD");
            revalidate();
            repaint();
        };

        mpesaRadio.addActionListener(e -> toggleMethod.run());
        emolaRadio.addActionListener(e -> toggleMethod.run());
        bankRadio.addActionListener(e -> toggleMethod.run());

        updateCalculations.run();
        toggleMethod.run();

        setContentPane(content);
    }

    private void recalculateTotal() {
        String planStr = getSelectedPlanKey();
        int months = getSelectedMonths();

        BigDecimal monthly = switch (planStr) {
            case "BASIC" -> new BigDecimal("1500.00");
            case "PRO" -> new BigDecimal("3500.00");
            case "ENTERPRISE" -> new BigDecimal("7500.00");
            default -> new BigDecimal("3500.00");
        };

        BigDecimal total = monthly.multiply(BigDecimal.valueOf(months));
        if (months >= 12) {
            total = total.multiply(new BigDecimal("0.85"));
            discountBadge.setText("(15% de Desconto Anual Aplicado)");
        } else if (months >= 6) {
            total = total.multiply(new BigDecimal("0.90"));
            discountBadge.setText("(10% de Desconto Semestral)");
        } else if (months >= 3) {
            total = total.multiply(new BigDecimal("0.95"));
            discountBadge.setText("(5% de Desconto Trimestral)");
        } else {
            discountBadge.setText("");
        }

        totalLabel.setText(String.format("%,.2f MT", total.setScale(2, RoundingMode.HALF_UP)));
    }

    private String getSelectedPlanKey() {
        int idx = planCombo.getSelectedIndex();
        return switch (idx) {
            case 1 -> "BASIC";
            case 2 -> "ENTERPRISE";
            default -> "PRO";
        };
    }

    private int getSelectedMonths() {
        int idx = periodCombo.getSelectedIndex();
        return switch (idx) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 12;
            default -> 1;
        };
    }

    private void processRenewal() {
        String planKey = getSelectedPlanKey();
        int months = getSelectedMonths();

        if (bankRadio.isSelected()) {
            String ref = refField.getText().trim();
            if (ref.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor introduza o número do comprovativo ou referência.",
                        "Referência Obrigatória", JOptionPane.WARNING_MESSAGE);
                return;
            }

            SelfServiceSubscriptionPaymentRequest req = new SelfServiceSubscriptionPaymentRequest(
                    planKey, months, "BANK_TRANSFER", null, ref, notesArea.getText().trim()
            );

            UIHelper.runWithProgress(this, "A submeter activação de plano",
                    () -> subscriptionClient.renewSubscription(req),
                    result -> {
                        if (result.success()) {
                            ToastManager.success(UIHelper.mainWindow, result.message());
                            if (onSuccess != null) onSuccess.accept(result.subscription());
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(this, result.message(), "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                    },
                    ex -> JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE)
            );
        } else {
            // Pagamento Móvel Push USSD
            MobilePaymentProvider prov = mpesaRadio.isSelected() ? MobilePaymentProvider.MPESA : MobilePaymentProvider.EMOLA;
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor introduza o número de telemóvel.",
                        "Telemóvel Obrigatório", JOptionPane.WARNING_MESSAGE);
                return;
            }

            BigDecimal totalAmount = parseAmountFromLabel(totalLabel.getText());
            String approvedRef = MobilePaymentModal.show(this, prov, totalAmount, posApiClient);
            if (approvedRef != null && !approvedRef.isEmpty()) {
                // Confirmar ativação no backend
                UIHelper.runWithProgress(this, "A activar plano no sistema",
                        () -> subscriptionClient.renewSubscription(new SelfServiceSubscriptionPaymentRequest(
                                planKey, months, prov.name(), phone, approvedRef, "Pagamento aprovado via " + prov.getDisplayName()
                        )),
                        result -> {
                            if (result.success()) {
                                ToastManager.success(UIHelper.mainWindow,
                                        "Plano " + planKey + " ativado com sucesso! Ref: " + approvedRef);
                                if (onSuccess != null) onSuccess.accept(result.subscription());
                                dispose();
                            }
                        },
                        ex -> JOptionPane.showMessageDialog(this, "Erro ao finalizar activação: " + ex.getMessage(),
                                "Erro", JOptionPane.ERROR_MESSAGE)
                );
            }
        }
    }

    private BigDecimal parseAmountFromLabel(String text) {
        try {
            String clean = text.replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
            return new BigDecimal(clean);
        } catch (Exception ex) {
            return new BigDecimal("3500.00");
        }
    }
}

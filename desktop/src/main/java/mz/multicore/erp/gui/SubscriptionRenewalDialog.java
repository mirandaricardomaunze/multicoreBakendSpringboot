package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SelfServiceSubscriptionPaymentRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.function.Consumer;

public final class SubscriptionRenewalDialog extends JDialog {

    private final MySubscriptionApiClient subscriptionClient;
    private final POSApiClient posApiClient;
    private final Consumer<MySubscriptionDTO> onSuccess;

    private final JComboBox<String> planCombo;
    private final JComboBox<String> periodCombo;
    private final JLabel totalLabel = new JLabel("3.500,00 MT");
    private final JLabel discountBadge = new JLabel("");

    private final JRadioButton mpesaRadio = new JRadioButton("M-Pesa (Push USSD)", true);
    private final JRadioButton emolaRadio = new JRadioButton("e-Mola (Push USSD)");
    private final JRadioButton bankRadio = new JRadioButton("Transferência Bancária / Comprovativo Manual");

    private final JTextField phoneField = new JTextField();
    private final JTextField refField = new JTextField();
    private final JTextArea notesArea = new JTextArea(2, 20);
    private final JPanel mobileCard;
    private final JPanel bankCard;
    private final ModernButton submitBtn;

    public static void show(Component parent, MySubscriptionApiClient subClient, Consumer<MySubscriptionDTO> onSuccess) {
        show(parent, subClient, subClient.getPosApiClient(), onSuccess);
    }

    public static void show(Component parent, MySubscriptionApiClient subClient, POSApiClient posClient, Consumer<MySubscriptionDTO> onSuccess) {
        new SubscriptionRenewalDialog(SwingUtilities.getWindowAncestor(parent), subClient, posClient, onSuccess).setVisible(true);
    }

    private SubscriptionRenewalDialog(Window owner, MySubscriptionApiClient subClient, POSApiClient posClient, Consumer<MySubscriptionDTO> onSuccess) {
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
        content.add(SubscriptionRenewalViewBuilder.buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        planCombo = new JComboBox<>(new String[]{"PRO — Profissional (3.500 MT/mês)", "BASIC — Básico (1.500 MT/mês)", "ENTERPRISE — Empresarial (7.500 MT/mês)"});
        UIHelper.styleComboBox(planCombo);
        periodCombo = new JComboBox<>(new String[]{"1 Mês (Sem desconto)", "3 Meses (5% de Desconto)", "6 Meses (10% de Desconto)", "12 Meses (15% Desconto Anual)"});
        UIHelper.styleComboBox(periodCombo);

        JPanel configForm = UIHelper.createDialogForm("Plano comercial pretendido:", planCombo, "Período de subscrição:", periodCombo);
        configForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(configForm);
        body.add(Box.createRigidArea(new Dimension(0, 10)));
        body.add(SubscriptionRenewalViewBuilder.buildTotalCard(discountBadge, totalLabel));
        body.add(Box.createRigidArea(new Dimension(0, 14)));

        JLabel methodTitle = new JLabel("Forma de Pagamento:");
        methodTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        methodTitle.setForeground(UIHelper.TEXT_LIGHT);
        methodTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(methodTitle);
        body.add(Box.createRigidArea(new Dimension(0, 6)));
        body.add(SubscriptionRenewalViewBuilder.buildRadioPanel(mpesaRadio, emolaRadio, bankRadio));
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        mobileCard = SubscriptionRenewalViewBuilder.buildMobileCard(phoneField);
        bankCard = SubscriptionRenewalViewBuilder.buildBankCard(refField, notesArea);
        body.add(mobileCard);
        body.add(bankCard);
        content.add(body, BorderLayout.CENTER);

        submitBtn = UIHelper.createPrimaryButton("Proceder ao Pagamento");
        submitBtn.setIcon(UIHelper.icon("fas-check-circle", 13, Color.WHITE));
        submitBtn.addActionListener(e -> processRenewal());
        content.add(SubscriptionRenewalViewBuilder.buildFooter(this::dispose, submitBtn), BorderLayout.SOUTH);

        planCombo.addActionListener(e -> recalculateTotal());
        periodCombo.addActionListener(e -> recalculateTotal());

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

        recalculateTotal();
        toggleMethod.run();
        setContentPane(content);
    }

    private void recalculateTotal() {
        String planKey = SubscriptionRenewalPricingCalculator.planKeyForIndex(planCombo.getSelectedIndex());
        int months = SubscriptionRenewalPricingCalculator.monthsForIndex(periodCombo.getSelectedIndex());
        BigDecimal total = SubscriptionRenewalPricingCalculator.calculateTotal(planKey, months);
        discountBadge.setText(SubscriptionRenewalPricingCalculator.discountLabelForMonths(months));
        totalLabel.setText(String.format("%,.2f MT", total));
    }

    private void processRenewal() {
        String planKey = SubscriptionRenewalPricingCalculator.planKeyForIndex(planCombo.getSelectedIndex());
        int months = SubscriptionRenewalPricingCalculator.monthsForIndex(periodCombo.getSelectedIndex());

        if (bankRadio.isSelected()) {
            String ref = refField.getText().trim();
            if (ref.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor introduza o número do comprovativo ou referência.", "Referência Obrigatória", JOptionPane.WARNING_MESSAGE);
                return;
            }
            SelfServiceSubscriptionPaymentRequest req = new SelfServiceSubscriptionPaymentRequest(planKey, months, "BANK_TRANSFER", null, ref, notesArea.getText().trim());
            UIHelper.runWithProgress(this, "A submeter activação de plano", () -> subscriptionClient.renewSubscription(req),
                    result -> {
                        if (result.success()) {
                            ToastManager.success(UIHelper.mainWindow, result.message());
                            if (onSuccess != null) onSuccess.accept(result.subscription());
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(this, result.message(), "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                    },
                    ex -> JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
        } else {
            MobilePaymentProvider prov = mpesaRadio.isSelected() ? MobilePaymentProvider.MPESA : MobilePaymentProvider.EMOLA;
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor introduza o número de telemóvel.", "Telemóvel Obrigatório", JOptionPane.WARNING_MESSAGE);
                return;
            }
            BigDecimal totalAmount = SubscriptionRenewalPricingCalculator.parseAmountFromLabel(totalLabel.getText());
            String approvedRef = MobilePaymentModal.show(this, prov, totalAmount, posApiClient);
            if (approvedRef != null && !approvedRef.isEmpty()) {
                UIHelper.runWithProgress(this, "A activar plano no sistema",
                        () -> subscriptionClient.renewSubscription(new SelfServiceSubscriptionPaymentRequest(planKey, months, prov.name(), phone, approvedRef, "Pagamento aprovado via " + prov.getDisplayName())),
                        result -> {
                            if (result.success()) {
                                ToastManager.success(UIHelper.mainWindow, "Plano " + planKey + " ativado com sucesso! Ref: " + approvedRef);
                                if (onSuccess != null) onSuccess.accept(result.subscription());
                                dispose();
                            }
                        },
                        ex -> JOptionPane.showMessageDialog(this, "Erro ao finalizar activação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            }
        }
    }
}

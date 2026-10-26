package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Diálogo especializado para registo de pagamentos e liquidação de assinaturas.
 */
public class RecordSubscriptionPaymentDialog {

    private final Window owner;
    private final PlatformApiClient platformApiClient;
    private final SubscriptionDTO sub;

    public RecordSubscriptionPaymentDialog(Window owner, PlatformApiClient platformApiClient, SubscriptionDTO sub) {
        this.owner = owner;
        this.platformApiClient = platformApiClient;
        this.sub = sub;
    }

    public boolean showDialog() {
        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"TRANSFERENCIA", "DINHEIRO", "MPESA", "EMOLA", "OUTRO"});
        UIHelper.styleComboBox(methodCombo);
        methodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("TRANSFERENCIA".equals(value)) setText("Transferência Bancária");
                else if ("DINHEIRO".equals(value)) setText("Numerário / Dinheiro");
                else if ("MPESA".equals(value)) setText("M-Pesa");
                else if ("EMOLA".equals(value)) setText("e-Mola");
                else if ("OUTRO".equals(value)) setText("Outro Meio");
                return this;
            }
        });

        JTextField referenceField = new JTextField();
        referenceField.setToolTipText("Ex: Nº talão de depósito, ID de transação M-Pesa/e-Mola, Nº de recibo...");
        UIHelper.styleTextField(referenceField);

        JComboBox<String> bankCombo = new JComboBox<>(new String[]{
                "Millennium BIM", "BCI", "Standard Bank", "Moza Banco", "Absa Bank", "Nedbank", "FNB", "Outro Banco"
        });
        UIHelper.styleComboBox(bankCombo);
        JTextField transferHolderField = new JTextField();
        transferHolderField.setToolTipText("Titular da conta ou Nº de conta bancária de origem");
        UIHelper.styleTextField(transferHolderField);
        JPanel transferPanel = UIHelper.createDialogForm("Banco:", bankCombo, "Titular / Conta de Origem:", transferHolderField);

        JComboBox<String> cashLocationCombo = new JComboBox<>(new String[]{
                "Sede / Escritório Principal", "Balcão Comercial", "Cobrança Externa / Caixa", "Outro Local"
        });
        UIHelper.styleComboBox(cashLocationCombo);
        JTextField manualReceiptField = new JTextField();
        manualReceiptField.setToolTipText("Nº do recibo manual ou talão físico entregue ao cliente");
        UIHelper.styleTextField(manualReceiptField);
        JPanel cashPanel = UIHelper.createDialogForm("Local / Caixa de Recebimento:", cashLocationCombo, "Nº Recibo Físico / Manual:", manualReceiptField);

        JTextField mobileNumberField = new JTextField();
        mobileNumberField.setToolTipText("Ex: 841234567 ou 851234567");
        UIHelper.styleTextField(mobileNumberField);
        JTextField mobileHolderField = new JTextField();
        mobileHolderField.setToolTipText("Nome do titular da conta móvel");
        UIHelper.styleTextField(mobileHolderField);
        JPanel mobilePanel = UIHelper.createDialogForm("Nº de Telemóvel:", mobileNumberField, "Titular da Conta Móvel:", mobileHolderField);

        JTextField otherDetailField = new JTextField();
        otherDetailField.setToolTipText("Descreva os detalhes e comprovativo do meio de pagamento");
        UIHelper.styleTextField(otherDetailField);
        JPanel otherPanel = UIHelper.createDialogForm("Descrição do Meio:", otherDetailField);

        CardLayout cardLayout = new CardLayout();
        JPanel detailsCardPanel = new JPanel(cardLayout);
        detailsCardPanel.setOpaque(false);
        detailsCardPanel.add(transferPanel, "TRANSFERENCIA");
        detailsCardPanel.add(cashPanel, "DINHEIRO");
        detailsCardPanel.add(mobilePanel, "MPESA");
        detailsCardPanel.add(mobilePanel, "EMOLA");
        detailsCardPanel.add(otherPanel, "OUTRO");

        methodCombo.addActionListener(e -> {
            String sel = (String) methodCombo.getSelectedItem();
            cardLayout.show(detailsCardPanel, sel != null ? sel : "TRANSFERENCIA");
            detailsCardPanel.revalidate();
            detailsCardPanel.repaint();
        });

        DateField paidAtField = new DateField(LocalDate.now());
        DateField startField = new DateField();
        DateField endField = new DateField();
        JTextField noteField = new JTextField();
        UIHelper.styleTextField(amountField);
        UIHelper.styleTextField(noteField);

        JPanel mainFields = UIHelper.createDialogForm(
                "Valor (MT) *:", amountField,
                "Método de Pagamento *:", methodCombo,
                "Referência (Opcional):", referenceField,
                "Pago em (AAAA-MM-DD) *:", paidAtField,
                "Período de (AAAA-MM-DD):", startField,
                "Período até (AAAA-MM-DD):", endField,
                "Observações:", noteField
        );

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        form.add(mainFields, BorderLayout.NORTH);
        form.add(detailsCardPanel, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(owner, "Registar Pagamento",
                "fas-money-bill-wave", sub.companyName() + " — o período até estende a validade", form)
                .setConfirmButton("Registar", "fas-check");
        dlg.setSize(580, 520);
        dlg.setOnSaveAsync(() -> {
            String method = (String) methodCombo.getSelectedItem();
            String details = switch (method != null ? method : "OUTRO") {
                case "TRANSFERENCIA" -> {
                    String bank = (String) bankCombo.getSelectedItem();
                    String holder = transferHolderField.getText().trim();
                    yield "Banco: " + bank + (holder.isEmpty() ? "" : " | Titular/Conta: " + holder);
                }
                case "DINHEIRO" -> {
                    String loc = (String) cashLocationCombo.getSelectedItem();
                    String rec = manualReceiptField.getText().trim();
                    yield "Local: " + loc + (rec.isEmpty() ? "" : " | Recibo: " + rec);
                }
                case "MPESA", "EMOLA" -> {
                    String phone = mobileNumberField.getText().trim();
                    String holder = mobileHolderField.getText().trim();
                    yield (phone.isEmpty() ? "" : "Telemóvel: " + phone)
                            + (holder.isEmpty() ? "" : (phone.isEmpty() ? "" : " | ") + "Titular: " + holder);
                }
                default -> otherDetailField.getText().trim();
            };

            String ref = referenceField.getText().trim();
            RecordPaymentRequest request = new RecordPaymentRequest(
                    parseAmount(amountField.getText(), "valor"),
                    method,
                    parseDate(paidAtField.getText()),
                    parseDate(startField.getText()),
                    parseDate(endField.getText()),
                    ref.isEmpty() ? null : ref,
                    details.isEmpty() ? null : details,
                    noteField.getText().trim()
            );
            return () -> platformApiClient.recordPayment(sub.companyId(), request);
        });

        return dlg.showDialog();
    }

    private static BigDecimal parseAmount(String text, String field) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para " + field + ".");
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }
}

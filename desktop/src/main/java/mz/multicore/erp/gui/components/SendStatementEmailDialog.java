package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.AccountStatementApiClient;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;
import mz.multicore.erp.modules.comercial.dto.SendStatementEmailRequest;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Diálogo modal para envio direto de extrato de conta corrente por correio eletrónico.
 * Permite ao operador validar o destinatário, assunto, nota de acompanhamento e disparar o envio.
 */
public class SendStatementEmailDialog {

    private final Window parent;
    private final AccountStatementApiClient apiClient;
    private final Long clientId;
    private final String clientName;
    private final String defaultEmail;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private JTextField emailField;
    private JTextField subjectField;
    private JTextArea messageArea;
    private EmailDispatchResultDTO lastResult;

    public SendStatementEmailDialog(
            Window parent,
            AccountStatementApiClient apiClient,
            Long clientId,
            String clientName,
            String defaultEmail,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.parent = parent;
        this.apiClient = apiClient;
        this.clientId = clientId;
        this.clientName = clientName;
        this.defaultEmail = defaultEmail != null ? defaultEmail : "";
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean showDialog() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 8, 0);
        g.gridx = 0;
        g.weightx = 1.0;

        JLabel lblEmail = new JLabel("Email do Destinatário *");
        lblEmail.setForeground(UIHelper.TEXT_LIGHT);
        lblEmail.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        emailField = new JTextField(defaultEmail);
        UIHelper.styleTextField(emailField);

        JLabel lblSubject = new JLabel("Assunto da Mensagem");
        lblSubject.setForeground(UIHelper.TEXT_LIGHT);
        lblSubject.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        String defaultSubj = "Extrato de Conta Corrente - " + (clientName != null ? clientName : "MULTICORE ERP");
        subjectField = new JTextField(defaultSubj);
        UIHelper.styleTextField(subjectField);

        JLabel lblMsg = new JLabel("Mensagem / Nota de Acompanhamento");
        lblMsg.setForeground(UIHelper.TEXT_LIGHT);
        lblMsg.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        messageArea = new JTextArea(4, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setText("Exmo.(a) Cliente,\nSegue em anexo o extrato atualizado da sua conta corrente para reconciliação e conferência.");
        messageArea.setBackground(UIHelper.FIELD_BG);
        messageArea.setForeground(UIHelper.TEXT_LIGHT);
        messageArea.setCaretColor(UIHelper.TEXT_LIGHT);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JScrollPane scrollMsg = new JScrollPane(messageArea);
        scrollMsg.setPreferredSize(new Dimension(380, 80));

        int row = 0;
        g.gridy = row++; form.add(lblEmail, g);
        g.gridy = row++; form.add(emailField, g);
        g.gridy = row++; form.add(lblSubject, g);
        g.gridy = row++; form.add(subjectField, g);
        g.gridy = row++; form.add(lblMsg, g);
        g.gridy = row++; form.add(scrollMsg, g);

        ModernFormDialog dlg = new ModernFormDialog(
                parent,
                "Enviar Extrato por Email",
                "fas-paper-plane",
                "Despacho oficial do documento em PDF para a caixa de correio do cliente",
                form
        );
        dlg.setConfirmButton("Enviar Extrato", "fas-paper-plane");

        dlg.setOnSaveAsync(() -> () -> {
            String email = emailField.getText() != null ? emailField.getText().trim() : "";
            if (email.isBlank() || !email.contains("@")) {
                throw new IllegalArgumentException("Por favor, introduza um endereço de correio eletrónico válido.");
            }
            SendStatementEmailRequest req = new SendStatementEmailRequest(
                    clientId,
                    email,
                    subjectField.getText(),
                    messageArea.getText(),
                    startDate,
                    endDate
            );
            lastResult = apiClient != null ? apiClient.sendCustomerStatementEmail(req) : null;
            return lastResult;
        });

        return dlg.showDialog();
    }

    public EmailDispatchResultDTO getLastResult() {
        return lastResult;
    }

    public JTextField getEmailField() { return emailField; }
    public JTextField getSubjectField() { return subjectField; }
    public JTextArea getMessageArea() { return messageArea; }
}

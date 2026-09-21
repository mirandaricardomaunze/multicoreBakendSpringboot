package mz.multicore.erp.gui.pos;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Diálogo modal para fecho cego de caixa, apuramento de divergência e emissão do Relatório Z.
 */
public class PosBlindCloseDialog extends JDialog {

    private final POSApiClient posApiClient;
    private final TillSessionDTO session;
    private final Runnable onCloseSuccess;

    private JTextField countedCashField;
    private JLabel lblStatusResult;
    private JLabel lblExpected;
    private JLabel lblCounted;
    private JLabel lblDiff;
    private JPanel resultPanel;
    private ModernButton btnConfirm;
    private ModernButton btnPrintZ;

    public PosBlindCloseDialog(Window owner, POSApiClient posApiClient, TillSessionDTO session, Runnable onCloseSuccess) {
        super(owner, "Fecho de Caixa — Relatório Z (Sessão #" + session.id() + ")", ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.session = session;
        this.onCloseSuccess = onCloseSuccess;

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createBodyPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.ROW_ALT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Fecho Cego de Caixa (Z)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitle = new JLabel("Operador: " + session.operator() + "  |  Abertura: " + session.openingBalance() + " MT");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createBodyPanel() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UIHelper.BG_DARK);
        body.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblInstruction = new JLabel("<html><b>Contagem Física da Gaveta</b><br>Introduza o valor total em numerário presente na caixa.</html>");
        lblInstruction.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblInstruction.setForeground(UIHelper.TEXT_LIGHT);
        lblInstruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblInstruction);

        body.add(Box.createVerticalStrut(15));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputPanel.setOpaque(false);
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAmount = new JLabel("Valor Contado (MT):");
        lblAmount.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblAmount.setForeground(UIHelper.TEXT_LIGHT);

        countedCashField = new JTextField(12);
        UIHelper.styleTextField(countedCashField);
        countedCashField.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));

        inputPanel.add(lblAmount);
        inputPanel.add(countedCashField);
        body.add(inputPanel);

        body.add(Box.createVerticalStrut(20));

        // Result panel (hidden before confirmation)
        resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayout(4, 1, 8, 8));
        resultPanel.setBackground(UIHelper.BG_CARD);
        resultPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setVisible(false);

        lblStatusResult = new JLabel("Sessão Fechada com Sucesso");
        lblStatusResult.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        lblStatusResult.setForeground(UIHelper.APPROVED_GREEN);

        lblExpected = new JLabel("Esperado pelo Sistema: —");
        lblExpected.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblExpected.setForeground(UIHelper.TEXT_LIGHT);

        lblCounted = new JLabel("Contado pelo Operador: —");
        lblCounted.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblCounted.setForeground(UIHelper.TEXT_LIGHT);

        lblDiff = new JLabel("Diferença: —");
        lblDiff.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblDiff.setForeground(UIHelper.TEXT_LIGHT);

        resultPanel.add(lblStatusResult);
        resultPanel.add(lblExpected);
        resultPanel.add(lblCounted);
        resultPanel.add(lblDiff);

        body.add(resultPanel);
        return body;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UIHelper.ROW_ALT);

        btnConfirm = UIHelper.createSuccessButton("Confirmar Fecho de Caixa");
        btnConfirm.setIcon(UIHelper.icon("fas-lock", 13, Color.WHITE));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        btnConfirm.addActionListener(e -> executeClose());

        btnPrintZ = UIHelper.createPrimaryButton("Imprimir Relatório Z (A4)");
        btnPrintZ.setIcon(UIHelper.icon("fas-print", 13, Color.WHITE));
        btnPrintZ.setForeground(Color.WHITE);
        btnPrintZ.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        btnPrintZ.setEnabled(false);
        btnPrintZ.addActionListener(e -> printZReport());

        ModernButton btnCancel = new ModernButton("Cancelar / Voltar", UIHelper.BUTTON_NEUTRAL, UIHelper.BUTTON_NEUTRAL_HOVER);
        btnCancel.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnPrintZ);
        footer.add(btnConfirm);
        footer.add(btnCancel);
        return footer;
    }

    private void executeClose() {
        String input = countedCashField.getText().trim().replace(",", ".");
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor introduza o montante contado na gaveta.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal countedCash;
        try {
            countedCash = new BigDecimal(input);
            if (countedCash.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Montante inválido. Introduza um valor numérico positivo.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            TillSessionDTO closed = posApiClient.closeSession(session.id(), countedCash, null);
            PosZReportDTO z = posApiClient.getZReport(session.id());

            lblExpected.setText(String.format("Esperado na Gaveta: %,.2f MT", z.expectedCash()));
            lblCounted.setText(String.format("Contado pelo Operador: %,.2f MT", countedCash));

            BigDecimal diff = z.difference() != null ? z.difference() : BigDecimal.ZERO;
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                lblDiff.setText("Diferença: 0.00 MT (Caixa Bate Certo)");
                lblDiff.setForeground(UIHelper.APPROVED_GREEN);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                lblDiff.setText(String.format("Diferença: %,.2f MT (Falta na Gaveta)", diff));
                lblDiff.setForeground(UIHelper.REJECTED_RED);
            } else {
                lblDiff.setText(String.format("Diferença: +%,.2f MT (Sobra na Gaveta)", diff));
                lblDiff.setForeground(UIHelper.ACCENT_BLUE);
            }

            resultPanel.setVisible(true);
            btnConfirm.setEnabled(false);
            countedCashField.setEnabled(false);
            btnPrintZ.setEnabled(true);

            if (onCloseSuccess != null) {
                onCloseSuccess.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao fechar caixa: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printZReport() {
        try {
            byte[] pdfBytes = posApiClient.renderZReport(session.id());
            UIHelper.previewOrPrintPdf(this, pdfBytes, "relatorio-z-" + session.id());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF do Relatório Z: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}

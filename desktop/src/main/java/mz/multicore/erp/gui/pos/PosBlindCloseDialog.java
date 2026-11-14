package mz.multicore.erp.gui.pos;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Diálogo modal para fecho cego de caixa (Blind Drop), contagem física discriminada
 * por notas e moedas de Meticais (MZN), apuramento de divergência e emissão do Relatório Z.
 * Delega contagem para {@link PosCashBreakdownPanel} e resultado para {@link PosBlindCloseResultPanel}.
 */
public class PosBlindCloseDialog extends JDialog {

    private final POSApiClient posApiClient;
    private final TillSessionDTO session;
    private final Runnable onCloseSuccess;
    private final PrintApiClient printApiClient;

    private final PosCashBreakdownPanel breakdownPanel = new PosCashBreakdownPanel();
    private final PosBlindCloseResultPanel resultPanel = new PosBlindCloseResultPanel();

    private JTextArea notesArea;
    private ModernButton btnConfirm;
    private ModernButton btnPrintZ;
    private ModernButton btnCancel;

    public PosBlindCloseDialog(Window owner, POSApiClient posApiClient, TillSessionDTO session, Runnable onCloseSuccess) {
        this(owner, posApiClient, session, onCloseSuccess, null);
    }

    public PosBlindCloseDialog(Window owner, POSApiClient posApiClient, TillSessionDTO session,
                               Runnable onCloseSuccess, PrintApiClient printApiClient) {
        super(owner, "Fecho Cego de Caixa — Sessão #" + session.id(), ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.session = session;
        this.onCloseSuccess = onCloseSuccess;
        this.printApiClient = printApiClient;

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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.ROW_ALT);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Fecho Cego de Caixa (Blind Drop)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setIcon(UIHelper.icon("fas-cash-register", 18, UIHelper.ACCENT_CYAN));
        title.setIconTextGap(10);

        String subtitleText = String.format("Operador: %s  |  Sessão #%d  |  Abertura: %,.2f MT",
                session.operator() != null ? session.operator() : "—",
                session.id(),
                session.openingBalance() != null ? session.openingBalance() : BigDecimal.ZERO);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 28, 0, 0));

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createContentPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        breakdownPanel.addTo(root);
        root.add(Box.createVerticalStrut(12));
        root.add(createNotesCard());
        root.add(Box.createVerticalStrut(12));
        root.add(resultPanel.component());

        JScrollPane scroll = new JScrollPane(root);
        UIHelper.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JPanel createNotesCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Observações / Justificação de Diferença (opcional):");
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lbl.setForeground(UIHelper.TEXT_LIGHT);

        notesArea = new JTextArea(2, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesArea.setBackground(UIHelper.BG_CARD);
        notesArea.setForeground(UIHelper.TEXT_LIGHT);
        notesArea.setCaretColor(Color.WHITE);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        card.add(lbl, BorderLayout.NORTH);
        card.add(notesArea, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.ROW_ALT);

        btnPrintZ = ButtonStyles.secondary(UIHelper.createPrimaryButton("Imprimir Relatório Z (A4)"));
        btnPrintZ.setIcon(UIHelper.icon("fas-print", 13, Color.WHITE));
        btnPrintZ.setEnabled(false);
        btnPrintZ.addActionListener(e -> printZReport());

        btnConfirm = ButtonStyles.main(UIHelper.createSuccessButton("Confirmar Fecho de Caixa"));
        btnConfirm.setIcon(UIHelper.icon("fas-lock", 13, Color.WHITE));
        btnConfirm.addActionListener(e -> executeClose());

        btnCancel = ButtonStyles.secondary(UIHelper.createDangerButton("Cancelar / Voltar"));
        btnCancel.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnPrintZ);
        footer.add(btnConfirm);
        footer.add(btnCancel);
        return footer;
    }

    private void executeClose() {
        BigDecimal countedCash;
        try {
            countedCash = breakdownPanel.calculateCountedAmount();
            if (countedCash.compareTo(BigDecimal.ZERO) < 0) {
                ToastManager.show(this, FeedbackType.WARNING, "O montante contado não pode ser negativo.");
                return;
            }
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Valor contado inválido. Introduza um montante numérico.");
            return;
        }

        String notes = notesArea.getText().trim();
        String json = breakdownPanel.buildDenominationsJson();

        try {
            posApiClient.closeSession(session.id(), countedCash, null, notes, json);
            PosZReportDTO z = posApiClient.getZReport(session.id());

            resultPanel.showResult(z, countedCash, notes);

            btnConfirm.setEnabled(false);
            btnPrintZ.setEnabled(true);
            breakdownPanel.setControlsEnabled(false);
            notesArea.setEnabled(false);
            btnCancel.setText("Fechar Janela");

            if (onCloseSuccess != null) {
                onCloseSuccess.run();
            }

            ToastManager.show(this, FeedbackType.SUCCESS, "Caixa fechado com sucesso! Pode agora imprimir o Relatório Z.");
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao fechar caixa: " + ex.getMessage());
        }
    }

    private void printZReport() {
        SensitivePrintCoordinator.print(this, printApiClient,
                mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.POS_Z_REPORT,
                session.id(), "Relatório Z da sessão " + session.id(),
                "relatorio-z-" + session.id(),
                () -> posApiClient.renderZReport(session.id()), null);
    }
}

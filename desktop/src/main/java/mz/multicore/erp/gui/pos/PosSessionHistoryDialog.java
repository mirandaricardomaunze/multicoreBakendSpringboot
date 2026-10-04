package mz.multicore.erp.gui.pos;

import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo modal para consulta e auditoria do histórico de fechos de caixa (Z) com re-impressão de Dossiê PDF.
 */
public class PosSessionHistoryDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final POSApiClient posApiClient;
    private final Long companyId;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<PosSessionSummaryDTO> currentList;

    public PosSessionHistoryDialog(Window owner, POSApiClient posApiClient, Long companyId) {
        super(owner, "Histórico & Auditoria de Fechos de Caixa (Z)", ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.companyId = companyId;

        setSize(980, 580);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        loadData();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.ROW_ALT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Histórico de Fechos de Caixa (Relatório Z)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitle = new JLabel("Consulta de sessões anteriores, reconciliação de gaveta e reimpressão de dossiês fiscais");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        String[] cols = {"ID", "Operador", "Abertura", "Fecho", "Esperado", "Contado", "Diferença", "Total Vendas", "Estado"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        // Align right for currency columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setForeground(UIHelper.TEXT_LIGHT);

        for (int c : new int[]{4, 5, 6, 7}) {
            table.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(UIHelper.BG_DARK);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UIHelper.ROW_ALT);

        ModernButton btnReload = UIHelper.createPrimaryButton("Recarregar");
        btnReload.setIcon(UIHelper.icon("fas-sync-alt", 13, Color.WHITE));
        btnReload.setForeground(Color.WHITE);
        btnReload.addActionListener(e -> loadData());

        ModernButton btnPrintZ = UIHelper.createPrimaryButton("Re-imprimir Relatório Z (A4)");
        btnPrintZ.setIcon(UIHelper.icon("fas-print", 13, Color.WHITE));
        btnPrintZ.setForeground(Color.WHITE);
        btnPrintZ.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        btnPrintZ.addActionListener(e -> printSelectedZ());

        ModernButton btnClose = UIHelper.createDangerButton("Fechar");
        btnClose.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> dispose());

        footer.add(btnReload);
        footer.add(btnPrintZ);
        footer.add(btnClose);
        return footer;
    }

    private void loadData() {
        try {
            currentList = posApiClient.getSessionsHistory(companyId);
            tableModel.setRowCount(0);

            for (PosSessionSummaryDTO s : currentList) {
                tableModel.addRow(new Object[]{
                        "#" + s.sessionId(),
                        s.operator(),
                        s.openDate() != null ? s.openDate().format(DATE_FMT) : "—",
                        s.closeDate() != null ? s.closeDate().format(DATE_FMT) : "— (Aberta)",
                        fmt(s.expectedCash()),
                        fmt(s.countedCash()),
                        fmtDiff(s.difference()),
                        fmt(s.totalSalesAmount()),
                        s.status()
                });
            }
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao carregar histórico de caixa: " + ex.getMessage());
        }
    }

    private void printSelectedZ() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || currentList == null || selectedRow >= currentList.size()) {
            ToastManager.show(this, FeedbackType.WARNING, "Seleccione uma sessão na tabela para reimprimir o Relatório Z.");
            return;
        }

        PosSessionSummaryDTO s = currentList.get(selectedRow);
        try {
            byte[] pdfBytes = posApiClient.renderZReport(s.sessionId());
            PrintPreviewDialog.show(this, pdfBytes, "relatorio-z-" + s.sessionId() + ".pdf");
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao gerar PDF do Relatório Z: " + ex.getMessage());
        }
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "—" : String.format("%,.2f MT", v);
    }

    private static String fmtDiff(BigDecimal d) {
        if (d == null) return "—";
        if (d.compareTo(BigDecimal.ZERO) == 0) return "0.00 MT";
        return (d.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + String.format("%,.2f MT", d);
    }
}

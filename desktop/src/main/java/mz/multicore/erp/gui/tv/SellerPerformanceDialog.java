package mz.multicore.erp.gui.tv;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diálogo modal executivo de Desempenho e Ranking da Força de Vendas.
 * Exibe o desempenho por vendedor a partir das faturas faturadas no período,
 * com ranking, percentagem de share, ticket médio e volume vendido.
 */
public class SellerPerformanceDialog extends JDialog {

    public record SellerStats(
            String sellerName,
            BigDecimal totalBilled,
            int invoiceCount,
            BigDecimal avgTicket,
            double sharePct
    ) {}

    public static void showDialog(Window parent, List<InvoiceDTO> invoices, String periodLabel, boolean lightMode) {
        SellerPerformanceDialog dialog = new SellerPerformanceDialog(parent, invoices, periodLabel, lightMode);
        dialog.setVisible(true);
    }

    public SellerPerformanceDialog(Window parent, List<InvoiceDTO> invoices, String periodLabel, boolean lightMode) {
        super(parent, "Desempenho da Força de Vendas • " + (periodLabel != null ? periodLabel : "Mês Atual"), ModalityType.APPLICATION_MODAL);

        Color bgColor = lightMode ? new Color(241, 245, 249) : new Color(15, 23, 42);
        Color cardBg = lightMode ? Color.WHITE : new Color(30, 41, 59);
        Color cardBorder = lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85);
        Color textLight = lightMode ? new Color(15, 23, 42) : new Color(248, 250, 252);
        Color textMuted = lightMode ? new Color(100, 116, 139) : new Color(148, 163, 184);

        getContentPane().setBackground(bgColor);
        setLayout(new BorderLayout(0, 12));

        // 1. Processar estatísticas de vendas por vendedor
        Map<String, BigDecimal> sellerRevenue = new HashMap<>();
        Map<String, Integer> sellerCount = new HashMap<>();
        BigDecimal totalTeamRevenue = BigDecimal.ZERO;

        if (invoices != null) {
            for (InvoiceDTO inv : invoices) {
                if (inv.totalAmount() == null) continue;
                String seller = inv.createdBy() != null && !inv.createdBy().isBlank() ? inv.createdBy().trim() : "Balcão / Geral";
                sellerRevenue.put(seller, sellerRevenue.getOrDefault(seller, BigDecimal.ZERO).add(inv.totalAmount()));
                sellerCount.put(seller, sellerCount.getOrDefault(seller, 0) + 1);
                totalTeamRevenue = totalTeamRevenue.add(inv.totalAmount());
            }
        }

        List<SellerStats> statsList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sellerRevenue.entrySet()) {
            String name = entry.getKey();
            BigDecimal rev = entry.getValue();
            int count = sellerCount.getOrDefault(name, 1);
            BigDecimal avg = count > 0 ? rev.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double share = totalTeamRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? rev.multiply(BigDecimal.valueOf(100)).divide(totalTeamRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            statsList.add(new SellerStats(name, rev, count, avg, share));
        }

        statsList.sort(Comparator.comparing(SellerStats::totalBilled).reversed());

        // 2. Cabeçalho e KPIs Globais
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(16, 18, 0, 18));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel titleLbl = new JLabel("DESEMPENHO INDIVIDUAL DA FORÇA DE VENDAS");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        titleLbl.setForeground(textLight);
        titleRow.add(titleLbl, BorderLayout.WEST);

        JLabel periodBadge = new JLabel("  PERÍODO: " + (periodLabel != null ? periodLabel.toUpperCase() : "MÊS ATUAL") + "  ");
        periodBadge.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        periodBadge.setForeground(UIHelper.PENDING_YELLOW);
        periodBadge.setOpaque(true);
        periodBadge.setBackground(new Color(245, 158, 11, 25));
        periodBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11, 80), 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        titleRow.add(periodBadge, BorderLayout.EAST);
        topPanel.add(titleRow);
        topPanel.add(Box.createVerticalStrut(12));

        // 4 KPIs
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);

        String topSellerName = statsList.isEmpty() ? "Sem vendas" : statsList.get(0).sellerName();
        BigDecimal topSellerRev = statsList.isEmpty() ? BigDecimal.ZERO : statsList.get(0).totalBilled();
        int totalInvoices = invoices != null ? invoices.size() : 0;
        BigDecimal teamAvgTicket = totalInvoices > 0
                ? totalTeamRevenue.divide(BigDecimal.valueOf(totalInvoices), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        kpiRow.add(buildKpiCard("FATURAMENTO DA EQUIPA", String.format("%,.2f MT", totalTeamRevenue), "Vendas globais no período", UIHelper.ACCENT_CYAN, cardBg, cardBorder, textLight, textMuted));
        kpiRow.add(buildKpiCard("Nº DE ATENDIMENTOS", String.valueOf(totalInvoices), "Total de faturas emitidas", UIHelper.APPROVED_GREEN, cardBg, cardBorder, textLight, textMuted));
        kpiRow.add(buildKpiCard("TICKET MÉDIO GERAL", String.format("%,.2f MT", teamAvgTicket), "Média por documento", UIHelper.KPI_PURPLE_SOFT, cardBg, cardBorder, textLight, textMuted));
        kpiRow.add(buildKpiCard("LÍDER DE VENDAS", topSellerName, String.format("%,.2f MT faturados", topSellerRev), UIHelper.PENDING_YELLOW, cardBg, cardBorder, textLight, textMuted));

        topPanel.add(kpiRow);
        add(topPanel, BorderLayout.NORTH);

        // 3. Tabela de Ranking da Força de Vendas
        JPanel centerCard = new ModernPanel(14, cardBg, cardBg);
        centerCard.setLayout(new BorderLayout(0, 10));
        centerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cardBorder, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel tableTitle = new JLabel("Classificação dos Vendedores por Faturamento");
        tableTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        tableTitle.setForeground(textLight);
        centerCard.add(tableTitle, BorderLayout.NORTH);

        String[] cols = {"Posição", "Vendedor / Operador", "Faturamento Realizado", "Atendimentos", "Ticket Médio", "Share de Vendas (%)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        int rank = 1;
        for (SellerStats s : statsList) {
            model.addRow(new Object[]{
                    rank + "º Lugar",
                    s.sellerName(),
                    String.format("%,.2f MT", s.totalBilled()),
                    s.invoiceCount() + " faturas",
                    String.format("%,.2f MT", s.avgTicket()),
                    String.format("%.1f%%", s.sharePct())
            });
            rank++;
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.setRowHeight(32);
        table.getTableHeader().setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        // Alinhamentos
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(cardBorder, 1, true));
        centerCard.add(scrollTable, BorderLayout.CENTER);

        // Nota sobre Metas e Bónus no módulo Desempenho
        JLabel helpNote = new JLabel("Dica: Para estipular metas mensais, bónus e comissões por vendedor, aceda a 'Gestão & CRM ➜ Desempenho ➜ Metas Comerciais'.");
        helpNote.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        helpNote.setForeground(textMuted);
        centerCard.add(helpNote, BorderLayout.SOUTH);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(0, 18, 0, 18));
        centerWrapper.add(centerCard, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // 4. Rodapé
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 18, 8, 18));

        ModernButton closeBtn = UIHelper.createSecondaryButton("Fechar Janela");
        closeBtn.setPreferredSize(new Dimension(120, 32));
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);

        add(footer, BorderLayout.SOUTH);

        UIHelper.applyStandardModalBounds(this, 880, 580);
    }

    private JPanel buildKpiCard(String title, String val, String sub, Color accent, Color bg, Color border, Color textLight, Color textMuted) {
        ModernPanel p = new ModernPanel(10, bg, bg);
        p.setLayout(new BorderLayout(0, 4));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        t.setForeground(textMuted);
        p.add(t, BorderLayout.NORTH);

        JLabel v = new JLabel(val);
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        v.setForeground(accent);
        p.add(v, BorderLayout.CENTER);

        JLabel s = new JLabel(sub);
        s.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        s.setForeground(textMuted);
        p.add(s, BorderLayout.SOUTH);

        return p;
    }
}

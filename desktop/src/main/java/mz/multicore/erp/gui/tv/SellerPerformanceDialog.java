package mz.multicore.erp.gui.tv;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
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
 * Modal executivo de desempenho e ranking da força de vendas, a partir das faturas do período.
 * Abre na moldura e no tema actual do Painel Executivo ({@link ExecutiveTvDialog}).
 */
public final class SellerPerformanceDialog {

    public record SellerStats(String sellerName, BigDecimal totalBilled, int invoiceCount,
                              BigDecimal avgTicket, double sharePct) {}

    private SellerPerformanceDialog() {}

    public static void showDialog(Window parent, List<InvoiceDTO> invoices, String periodLabel, boolean lightMode) {
        String period = periodLabel != null ? periodLabel : "Este mês";
        new ExecutiveTvDialog(parent, "Desempenho da força de vendas", "fas-user-tie", "Período: " + period, lightMode)
                .content(buildContent(invoices, period, lightMode)).closeOnly("Fechar").show(920, 620);
    }

    /** Conteúdo do modal, no tema indicado (testável sem abrir a janela). */
    static JPanel buildContent(List<InvoiceDTO> invoices, String period, boolean lightMode) {
        List<SellerStats> stats = rank(invoices);
        BigDecimal teamTotal = stats.stream().map(SellerStats::totalBilled).reduce(BigDecimal.ZERO, BigDecimal::add);
        int count = invoices != null ? invoices.size() : 0;
        BigDecimal avg = count > 0 ? teamTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        JPanel content = ExecutiveTvComponents.transparent(new BorderLayout(0, 14));
        JPanel kpis = ExecutiveTvComponents.transparent(new GridLayout(1, 4, ExecutiveTvComponents.GAP, 0));
        kpis.add(ExecutiveTvComponents.kpiTile("fas-coins", "Faturação da equipa", DisplayNumbers.money(teamTotal) + " MT",
                "Vendas no período", UIHelper.ACCENT_CYAN, lightMode));
        kpis.add(ExecutiveTvComponents.kpiTile("fas-receipt", "Atendimentos", String.valueOf(count),
                "Faturas emitidas", UIHelper.APPROVED_GREEN, lightMode));
        kpis.add(ExecutiveTvComponents.kpiTile("fas-balance-scale", "Valor médio", DisplayNumbers.money(avg) + " MT",
                "Por documento", UIHelper.KPI_PURPLE_SOFT, lightMode));
        kpis.add(ExecutiveTvComponents.kpiTile("fas-trophy", "Líder de vendas",
                stats.isEmpty() ? "Sem vendas" : stats.get(0).sellerName(),
                stats.isEmpty() ? "—" : DisplayNumbers.money(stats.get(0).totalBilled()) + " MT", UIHelper.PENDING_YELLOW, lightMode));
        content.add(kpis, BorderLayout.NORTH);

        JPanel card = ExecutiveTvComponents.card(lightMode, new BorderLayout(0, 10), 14);
        card.add(ExecutiveTvComponents.sectionHeader("Classificação por faturação", period, lightMode), BorderLayout.NORTH);
        card.add(ExecutiveTvFormControls.table(new JTable(tableModel(stats)), lightMode, new int[]{0, 3, 5}, new int[]{2, 4}),
                BorderLayout.CENTER);
        card.add(ExecutiveTvComponents.label("Metas, bónus e comissões por vendedor: Gestão & CRM › Desempenho › Metas Comerciais.",
                Font.PLAIN, 11, ExecutiveTvTheme.getTextMutedColor(lightMode)), BorderLayout.SOUTH);
        content.add(card, BorderLayout.CENTER);
        return content;
    }

    /** Vendas por operador do período, da maior para a menor. Sem operador conta como "Balcão / Geral". */
    static List<SellerStats> rank(List<InvoiceDTO> invoices) {
        Map<String, BigDecimal> revenue = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceDTO invoice : invoices == null ? List.<InvoiceDTO>of() : invoices) {
            if (invoice.totalAmount() == null) continue;
            String seller = invoice.createdBy() != null && !invoice.createdBy().isBlank() ? invoice.createdBy().trim() : "Balcão / Geral";
            revenue.merge(seller, invoice.totalAmount(), BigDecimal::add);
            counts.merge(seller, 1, Integer::sum);
            total = total.add(invoice.totalAmount());
        }
        List<SellerStats> stats = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revenue.entrySet()) {
            int n = counts.getOrDefault(entry.getKey(), 1);
            BigDecimal avg = entry.getValue().divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            double share = total.signum() > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
            stats.add(new SellerStats(entry.getKey(), entry.getValue(), n, avg, share));
        }
        stats.sort(Comparator.comparing(SellerStats::totalBilled).reversed());
        return stats;
    }

    private static DefaultTableModel tableModel(List<SellerStats> stats) {
        String[] cols = {"Posição", "Vendedor / Operador", "Faturação", "Atendimentos", "Valor médio", "Quota (%)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        int position = 1;
        for (SellerStats s : stats) {
            model.addRow(new Object[]{position++ + "º", s.sellerName(), DisplayNumbers.money(s.totalBilled()) + " MT",
                    String.valueOf(s.invoiceCount()), DisplayNumbers.money(s.avgTicket()) + " MT",
                    DisplayNumbers.qty(BigDecimal.valueOf(s.sharePct())) + "%"});
        }
        return model;
    }
}

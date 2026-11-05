package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Coordenador de apresentação, KPIs, chips de resumo e mapeamento de dados do histórico POS. */
final class PosSalesHistoryViewCoordinator {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JLabel salesCountValue = new JLabel("0 vendas");
    private final JLabel salesTotalValue = new JLabel("0,00 MT");
    private final JLabel salesVariationValue = new JLabel("—");
    private final JLabel salesHistorySummary = new JLabel(" ");

    record DateRange(LocalDate from, LocalDate to) {}

    PosSalesHistoryViewCoordinator() {
        salesCountValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        salesCountValue.setForeground(UIHelper.TEXT_LIGHT);
        salesTotalValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        salesTotalValue.setForeground(UIHelper.APPROVED_GREEN);
        salesVariationValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        salesVariationValue.setForeground(UIHelper.ACCENT_BLUE);
        salesHistorySummary.setForeground(UIHelper.TEXT_LIGHT);
        salesHistorySummary.setBorder(new EmptyBorder(8, 8, 8, 8));
    }

    JLabel summaryLabel() { return salesHistorySummary; }

    JPanel buildKpiBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 4, 0));
        bar.add(createSummaryChip("fas-receipt", "Vendas:", salesCountValue));
        bar.add(createSummaryChip("fas-cash-register", "Total POS:", salesTotalValue));
        bar.add(createSummaryChip("fas-chart-line", "Variação:", salesVariationValue));
        return bar;
    }

    private JPanel createSummaryChip(String iconCode, String caption, JLabel value) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chip.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon(iconCode, 13, UIHelper.TEXT_MUTED));
        JLabel cap = new JLabel(caption);
        cap.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        cap.setForeground(UIHelper.TEXT_MUTED);
        chip.add(icon); chip.add(cap); chip.add(value);
        return chip;
    }

    /** Linha do histórico: comprador real (cliente não cadastrado incluído), valor entregue e troco gravados. */
    static Object[] historyRow(InvoiceDTO inv, PosSalePaymentSummaryDTO payment) {
        String buyer = inv.buyerName();
        String buName = inv.businessUnitName() != null && !inv.businessUnitName().isBlank() ? inv.businessUnitName() : "—";
        return new Object[]{
                inv.id(), inv.invoiceNumber(),
                inv.createdAt() != null ? inv.createdAt().format(DTF) : "—",
                inv.createdBy() != null ? inv.createdBy() : "—",
                buyer != null && !buyer.isBlank() ? buyer : "—",
                inv.totalAmount(),
                payment != null ? payment.tenderedAmount() : "—",
                payment != null ? payment.changeGiven() : "—",
                inv.status() != null ? inv.status().name() : "—", buName
        };
    }

    DateRange selectedDateRange(JComboBox<String> periodCombo) {
        String option = periodCombo == null || periodCombo.getSelectedItem() == null
                ? "Todo o período" : String.valueOf(periodCombo.getSelectedItem());
        LocalDate today = LocalDate.now();
        return switch (option) {
            case "Hoje" -> new DateRange(today, today);
            case "Últimos 7 dias" -> new DateRange(today.minusDays(6), today);
            case "Últimos 30 dias" -> new DateRange(today.minusDays(29), today);
            case "Este mês" -> new DateRange(today.withDayOfMonth(1), today);
            default -> new DateRange(null, null);
        };
    }

    void applySalesHistory(POSPanel owner, List<InvoiceDTO> loaded, long totalOnServer,
                           POSSalesSummaryDTO summary, JComboBox<String> businessUnitCombo,
                           Map<Long, PosSalePaymentSummaryDTO> payments) {
        owner.salesHistoryList = loaded;
        owner.salesHistoryModel.setRowCount(0);
        for (var inv : owner.salesHistoryList) {
            owner.salesHistoryModel.addRow(historyRow(inv, payments == null ? null : payments.get(inv.id())));
        }
        BigDecimal total = owner.salesHistoryList.stream()
                .map(InvoiceDTO::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        salesHistorySummary.setText(String.format(
                "<html><b>%d</b> vendas POS nesta página (de %d) — total da página <b>%,.2f MT</b></html>",
                owner.salesHistoryList.size(), totalOnServer, total));
        applySummary(summary);
        if (businessUnitCombo != null) {
            Object prev = businessUnitCombo.getSelectedItem();
            Set<String> bus = new TreeSet<>();
            for (var inv : owner.salesHistoryList) {
                if (inv.businessUnitName() != null && !inv.businessUnitName().isBlank()) {
                    bus.add(inv.businessUnitName());
                }
            }
            businessUnitCombo.removeAllItems();
            businessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) businessUnitCombo.addItem(b);
            if (prev != null) businessUnitCombo.setSelectedItem(prev);
        }
    }

    private void applySummary(POSSalesSummaryDTO summary) {
        if (summary == null) return;
        salesCountValue.setText(summary.count() + (summary.count() == 1 ? " venda" : " vendas"));
        BigDecimal safeTotal = summary.totalAmount() == null ? BigDecimal.ZERO : summary.totalAmount();
        salesTotalValue.setText(String.format("%,.2f MT", safeTotal));
        salesVariationValue.setText(summary.totalVariationPercent() == null
                ? "Sem comparativo" : signedPercent(summary.totalVariationPercent()));
    }

    private String signedPercent(BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : "";
        return sign + value.stripTrailingZeros().toPlainString() + "%";
    }
}

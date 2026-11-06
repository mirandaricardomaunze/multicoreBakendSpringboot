package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
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

/** Coordenador de apresentação, cartões KPI e mapeamento de dados do histórico POS. */
final class PosSalesHistoryViewCoordinator {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final JLabel salesCountValue = new JLabel("0");
    private final JLabel salesTotalValue = new JLabel("0,00 MT");
    private final JLabel salesVariationValue = new JLabel("—");
    private final JLabel pageTotalValue = new JLabel("0,00 MT");
    private final JLabel pageSubtitle = new JLabel("Nenhuma venda nesta página");

    record DateRange(LocalDate from, LocalDate to) {}

    /** Cartões de resumo no padrão dos outros ecrãs (KpiCard), em vez de texto solto. */
    JPanel buildKpiBar() {
        JPanel grid = KpiCard.createGrid(4);
        grid.add(KpiCard.createMetricCard("Vendas no Período", salesCountValue, "Vendas POS registadas", "fas-receipt", UIHelper.ACCENT_BLUE));
        grid.add(KpiCard.createMetricCard("Total POS", salesTotalValue, "Facturado no período", "fas-cash-register", UIHelper.APPROVED_GREEN));
        grid.add(KpiCard.createMetricCard("Variação", salesVariationValue, "Face ao período anterior", "fas-chart-line", UIHelper.ACCENT_CYAN));
        grid.add(KpiCard.createMetricCard("Total da Página", pageTotalValue, pageSubtitle, "fas-file-invoice-dollar", UIHelper.PENDING_YELLOW));
        grid.setBorder(new EmptyBorder(0, 0, 10, 0));
        return grid;
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
        pageTotalValue.setText(String.format("%,.2f MT", total));
        pageSubtitle.setText(owner.salesHistoryList.size() + " de " + totalOnServer
                + (totalOnServer == 1 ? " venda" : " vendas"));
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
        salesCountValue.setText(String.valueOf(summary.count()));
        BigDecimal safeTotal = summary.totalAmount() == null ? BigDecimal.ZERO : summary.totalAmount();
        salesTotalValue.setText(String.format("%,.2f MT", safeTotal));
        salesVariationValue.setText(summary.totalVariationPercent() == null
                ? "Sem comparativo" : signedPercent(summary.totalVariationPercent()));
        BigDecimal variation = summary.totalVariationPercent();
        if (variation != null) salesVariationValue.setForeground(variation.signum() < 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
    }

    private String signedPercent(BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : "";
        return sign + value.stripTrailingZeros().toPlainString() + "%";
    }
}

package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Histórico pesquisável das vendas do POS. */
final class PosSalesHistoryPanel {
    private final POSPanel owner;
    private JLabel salesHistorySummary;
    private TablePager pager;
    private JComboBox<String> periodCombo;
    private JLabel salesCountValue;
    private JLabel salesTotalValue;
    private JLabel salesVariationValue;
    PosSalesHistoryPanel(POSPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        String[] cols = {"ID", "Nº Venda", "Data", "Operador", "Cliente", "Total", "Estado"};
        owner.salesHistoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.salesHistoryTable = new JTable(owner.salesHistoryModel);
        owner.salesHistoryTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(owner.salesHistoryTable);
        owner.salesHistoryTable.getColumnModel().getColumn(5)
                .setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.money());
        owner.salesHistoryTable.getColumnModel().getColumn(6)
                .setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.status());
        owner.salesHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        owner.salesHistoryTable.setFillsViewportHeight(true);
        // Esconder coluna ID
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMinWidth(0);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setWidth(0);
        // Larguras proporcionais
        owner.salesHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(140);  // Nº Venda
        owner.salesHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(120);  // Data
        owner.salesHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Operador
        owner.salesHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(160);  // Cliente
        owner.salesHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Total
        owner.salesHistoryTable.getColumnModel().getColumn(6).setPreferredWidth(80);   // Estado

        JScrollPane scroll = new JScrollPane(owner.salesHistoryTable);
        UIHelper.styleScrollPane(scroll);

        salesHistorySummary = new JLabel(" ");
        salesHistorySummary.setForeground(UIHelper.TEXT_LIGHT);
        salesHistorySummary.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel kpiBar = buildKpiBar();

        ModernButton reprintBtn = UIHelper.createPrimaryButton("Reimprimir Recibo");
        reprintBtn.setIcon(UIHelper.icon("fas-print", 14));
        reprintBtn.addActionListener(e -> {
            int row = TableFilter.selectedModelRow(owner.salesHistoryTable);
            if (row < 0) {
                owner.showPosNotice(FeedbackType.WARNING, "Seleccione uma venda", "Escolha uma venda no histórico para continuar.");
                return;
            }
            Long invoiceId = (Long) owner.salesHistoryModel.getValueAt(row, 0);
            String invNum = String.valueOf(owner.salesHistoryModel.getValueAt(row, 1));
            UIHelper.runWithProgress(owner, "A gerar recibo…",
                    () -> owner.posApiClient.renderReceipt(invoiceId),
                    pdf -> PrintPreviewDialog.show(owner, pdf, "recibo-" + invNum),
                    ex -> owner.showPosNotice(FeedbackType.ERROR, "Não foi possível gerar o recibo", ex.getMessage()));
        });

        ModernButton returnBtn = new ModernButton("Devolver / Trocar", UIHelper.BUTTON_NEUTRAL, UIHelper.BUTTON_NEUTRAL_HOVER);
        returnBtn.setIcon(UIHelper.icon("fas-undo", 14, Color.WHITE));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.addActionListener(e -> owner.showReturnDialog());

        ModernButton refreshBtn = new ModernButton("Actualizar", UIHelper.BUTTON_NEUTRAL, UIHelper.BUTTON_NEUTRAL_HOVER);
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14, Color.WHITE));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);
        buttons.add(refreshBtn);
        buttons.add(returnBtn);
        buttons.add(reprintBtn);

        JTextField shSearch = TableFilter.searchField("Nº venda, operador ou cliente…");
        JComboBox<String> shEstado = TableFilter.combo("Todos os estados",
                "PAID", "APPROVED", "PARTIALLY_PAID", "CANCELLED");
        periodCombo = TableFilter.periodCombo();
        TableFilter.install(owner.salesHistoryTable, shSearch,
                java.util.List.of(new TableFilter.ColumnFilter(shEstado, 6)),
                java.util.List.of());
        periodCombo.addActionListener(e -> {
            if (pager != null) pager.reload();
        });
        JPanel shBar = TableFilter.bar(shSearch,
                TableFilter.label("Estado:"), shEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), periodCombo);
        shBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        JPanel summaryPanel = new JPanel(new BorderLayout(0, 8));
        summaryPanel.setOpaque(false);
        summaryPanel.add(kpiBar, BorderLayout.NORTH);
        summaryPanel.add(salesHistorySummary, BorderLayout.SOUTH);
        north.add(summaryPanel, BorderLayout.NORTH);
        north.add(shBar, BorderLayout.SOUTH);

        // O histórico do POS é a listagem que mais cresce numa loja: vem paginado do servidor.
        pager = new TablePager(this::loadPage);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(pager, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 5, 5, 5));
        content.add(north, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);
        return content;
    }

    public void refresh() {
        if (owner.salesHistoryModel == null || salesHistorySummary == null) return;
        pager.reload();
    }

    private void loadPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        DateRange range = selectedDateRange();
        UIHelper.loadAsync(owner, () -> new SalesHistoryData(
                        owner.comercialApiClient.getPOSSalesPage(companyId, page, size, range.from(), range.to()),
                        owner.comercialApiClient.getPOSSalesSummary(companyId, range.from(), range.to())),
                data -> {
                    pager.apply(data.page());
                    applySalesHistory(data.page().items(), data.page().totalElements(), data.summary());
                },
                error -> owner.showPosLoadError("histórico de vendas", error));
    }

    private void applySalesHistory(java.util.List<InvoiceDTO> loaded, long totalOnServer, POSSalesSummaryDTO summary) {
        owner.salesHistoryList = loaded;
        java.time.format.DateTimeFormatter dtf =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        owner.salesHistoryModel.setRowCount(0);
        for (var inv : owner.salesHistoryList) {
            owner.salesHistoryModel.addRow(new Object[]{
                    inv.id(),
                    inv.invoiceNumber(),
                    inv.createdAt() != null ? inv.createdAt().format(dtf) : "—",
                    inv.createdBy() != null ? inv.createdBy() : "—",
                    inv.clientName() != null ? inv.clientName() : "—",
                    inv.totalAmount(),
                    inv.status() != null ? inv.status().name() : "—"
            });
        }
        BigDecimal total = owner.salesHistoryList.stream()
                .map(InvoiceDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // O total é o DESTA PÁGINA — dizê-lo evita que se leia como o total da loja.
        salesHistorySummary.setText(String.format(
                "<html><b>%d</b> vendas POS nesta página (de %d) — total da página <b>%,.2f MT</b></html>",
                owner.salesHistoryList.size(), totalOnServer, total));
        applySummary(summary);
    }

    private DateRange selectedDateRange() {
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

    private record DateRange(LocalDate from, LocalDate to) {}

    private JPanel buildKpiBar() {
        salesCountValue = new JLabel("0 vendas");
        salesCountValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        salesCountValue.setForeground(UIHelper.TEXT_LIGHT);

        salesTotalValue = new JLabel("0,00 MT");
        salesTotalValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        salesTotalValue.setForeground(UIHelper.APPROVED_GREEN);

        salesVariationValue = new JLabel("—");
        salesVariationValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        salesVariationValue.setForeground(UIHelper.ACCENT_BLUE);

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
        chip.add(icon);
        chip.add(cap);
        chip.add(value);
        return chip;
    }

    private void applySummary(POSSalesSummaryDTO summary) {
        if (summary == null) return;
        salesCountValue.setText(summary.count() + (summary.count() == 1 ? " venda" : " vendas"));
        salesTotalValue.setText(String.format("%,.2f MT", safe(summary.totalAmount())));
        salesVariationValue.setText(summary.totalVariationPercent() == null
                ? "Sem comparativo"
                : signedPercent(summary.totalVariationPercent()));
    }

    private String variationText(BigDecimal percent, String fallback) {
        return percent == null ? fallback : signedPercent(percent) + " · " + fallback;
    }

    private String signedPercent(BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : "";
        return sign + value.stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record SalesHistoryData(
            mz.multicore.erp.architecture.paging.PageResponse<InvoiceDTO> page,
            POSSalesSummaryDTO summary) {}

}

package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.modules.pos.dto.PosSalePaymentSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Histórico pesquisável das vendas do POS. */
final class PosSalesHistoryPanel {
    private final POSPanel owner;
    private final PosSalesHistoryViewCoordinator viewCoordinator = new PosSalesHistoryViewCoordinator();
    private TablePager pager;
    private JComboBox<String> periodCombo;
    private JComboBox<String> shBusinessUnitCombo;

    PosSalesHistoryPanel(POSPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        String[] cols = {"ID", "Nº Venda", "Data", "Operador", "Cliente", "Total", "Entregue", "Troco", "Estado", "Negócio"};
        owner.salesHistoryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.salesHistoryTable = new JTable(owner.salesHistoryModel);
        owner.salesHistoryTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(owner.salesHistoryTable);
        for (int moneyCol : new int[]{5, 6, 7}) {
            owner.salesHistoryTable.getColumnModel().getColumn(moneyCol)
                    .setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.money());
        }
        owner.salesHistoryTable.getColumnModel().getColumn(8)
                .setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.status());
        owner.salesHistoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        owner.salesHistoryTable.setFillsViewportHeight(true);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMinWidth(0); // coluna ID escondida
        owner.salesHistoryTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.salesHistoryTable.getColumnModel().getColumn(0).setWidth(0);
        int[] widths = {0, 140, 120, 100, 160, 100, 100, 90, 80, 110};
        for (int c = 1; c < widths.length; c++) owner.salesHistoryTable.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);

        JScrollPane scroll = new JScrollPane(owner.salesHistoryTable);
        UIHelper.styleScrollPane(scroll);

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
            SensitivePrintCoordinator.print(owner, owner.printApiClient,
                    mz.multicore.erp.modules.printing.dto.SensitiveDocumentType.RECEIPT,
                    invoiceId, "Recibo da venda " + invNum, "recibo-" + invNum,
                    () -> owner.posApiClient.renderReceipt(invoiceId), this::refresh);
        });

        ModernButton returnBtn = new ModernButton("Devolver / Trocar", UIHelper.ACCENT_ORANGE, UIHelper.ACCENT_ORANGE_HOVER);
        returnBtn.setIcon(UIHelper.icon("fas-undo", 14, Color.WHITE));
        returnBtn.setForeground(Color.WHITE);
        returnBtn.addActionListener(e -> owner.showReturnDialog());


        JTextField shSearch = TableFilter.searchField("Nº venda, operador ou cliente");
        JComboBox<String> shEstado = TableFilter.combo("Todos os estados",
                "Paga", "Aprovado", "Parcialmente paga", "Cancelado");
        UIHelper.styleComboBox(shEstado);
        shEstado.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        shBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        periodCombo = TableFilter.periodCombo();
        UIHelper.styleComboBox(periodCombo);
        periodCombo.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(owner.salesHistoryTable, shSearch,
                java.util.List.of(
                        new TableFilter.ColumnFilter(shEstado, 8),
                        new TableFilter.ColumnFilter(shBusinessUnitCombo, 9)),
                java.util.List.of());
        periodCombo.addActionListener(e -> { if (pager != null) pager.reload(); });

        UIHelper.ensureHeadersFit(owner.salesHistoryTable);
        JPanel shBar = TableFilter.bar(shSearch, TableFilter.label("Negócio:"), shBusinessUnitCombo,
                TableFilter.label("Estado:"), shEstado, TableFilter.label("Período:"), periodCombo);

        pager = new TablePager(this::loadPage);

        // Como em Pedidos: título e acções no cabeçalho do card, filtros numa linha por baixo.
        ModernPanel tableCard = new ModernPanel(16);
        tableCard.setLayout(new BorderLayout(0, 6));
        tableCard.setBorder(new EmptyBorder(8, 12, 8, 12));
        tableCard.add(UIHelper.tableCardTop("Vendas Registadas", shBar,
                UIHelper.createRefreshButton(this::refresh), returnBtn, reprintBtn), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(pager, BorderLayout.SOUTH);
        UIHelper.installQuickPeek(owner.salesHistoryTable, tableCard);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(viewCoordinator.buildKpiBar(), BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        return content;
    }

    public void refresh() {
        if (owner.salesHistoryModel == null || pager == null) return;
        pager.reload();
    }

    private void loadPage(int page, int size) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        var range = viewCoordinator.selectedDateRange(periodCombo);
        UIHelper.loadAsync(owner, () -> {
                    var salesPage = owner.comercialApiClient.getPOSSalesPage(companyId, page, size, range.from(), range.to());
                    return new SalesHistoryData(salesPage,
                            owner.comercialApiClient.getPOSSalesSummary(companyId, range.from(), range.to()),
                            loadPayments(salesPage.items()));
                },
                data -> {
                    pager.apply(data.page());
                    viewCoordinator.applySalesHistory(owner, data.page().items(), data.page().totalElements(),
                            data.summary(), shBusinessUnitCombo, data.payments());
                },
                error -> owner.showPosLoadError("histórico de vendas", error));
    }

    /** Sem esta informação o histórico continua utilizável: as colunas Entregue e Troco ficam com "—". */
    private java.util.Map<Long, PosSalePaymentSummaryDTO> loadPayments(java.util.List<InvoiceDTO> sales) {
        if (owner.posApiClient == null || sales == null || sales.isEmpty()) return java.util.Map.of();
        try {
            return owner.posApiClient.getSalePaymentSummaries(sales.stream().map(InvoiceDTO::id).toList()).stream()
                    .collect(java.util.stream.Collectors.toMap(PosSalePaymentSummaryDTO::invoiceId, p -> p, (a, b) -> a));
        } catch (RuntimeException ex) {
            return java.util.Map.of();
        }
    }

    private record SalesHistoryData(
            mz.multicore.erp.architecture.paging.PageResponse<InvoiceDTO> page,
            POSSalesSummaryDTO summary,
            java.util.Map<Long, PosSalePaymentSummaryDTO> payments) {}
}

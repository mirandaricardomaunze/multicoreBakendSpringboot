package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;

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
        String[] cols = {"ID", "Nº Venda", "Data", "Operador", "Cliente", "Total", "Estado", "Negócio"};
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
        owner.salesHistoryTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        owner.salesHistoryTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        owner.salesHistoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        owner.salesHistoryTable.getColumnModel().getColumn(4).setPreferredWidth(160);
        owner.salesHistoryTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        owner.salesHistoryTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        owner.salesHistoryTable.getColumnModel().getColumn(7).setPreferredWidth(110);

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

        ModernButton refreshBtn = UIHelper.createPrimaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14, Color.WHITE));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.addActionListener(e -> refresh());

        JPanel buttons = UIHelper.actionsBar(refreshBtn, returnBtn, reprintBtn);

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
                        new TableFilter.ColumnFilter(shEstado, 6),
                        new TableFilter.ColumnFilter(shBusinessUnitCombo, 7)),
                java.util.List.of());
        periodCombo.addActionListener(e -> {
            if (pager != null) pager.reload();
        });

        UIHelper.ensureHeadersFit(owner.salesHistoryTable);
        JPanel shBar = TableFilter.bar(
                shSearch,
                TableFilter.label("Negócio:"), shBusinessUnitCombo,
                TableFilter.label("Estado:"), shEstado,
                TableFilter.label("Período:"), periodCombo
        );
        shBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        pager = new TablePager(this::loadPage);

        ModernPanel tableCard = new ModernPanel(16);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        tableCard.add(shBar, BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(pager, BorderLayout.SOUTH);
        UIHelper.installQuickPeek(owner.salesHistoryTable, tableCard);

        JPanel summaryPanel = new JPanel(new BorderLayout(0, 4));
        summaryPanel.setOpaque(false);
        summaryPanel.add(viewCoordinator.buildKpiBar(), BorderLayout.NORTH);
        summaryPanel.add(viewCoordinator.summaryLabel(), BorderLayout.SOUTH);

        JPanel topHeader = new JPanel(new BorderLayout(12, 8));
        topHeader.setOpaque(false);
        topHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        topHeader.add(summaryPanel, BorderLayout.CENTER);
        topHeader.add(buttons, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        content.add(topHeader, BorderLayout.NORTH);
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
        UIHelper.loadAsync(owner, () -> new SalesHistoryData(
                        owner.comercialApiClient.getPOSSalesPage(companyId, page, size, range.from(), range.to()),
                        owner.comercialApiClient.getPOSSalesSummary(companyId, range.from(), range.to())),
                data -> {
                    pager.apply(data.page());
                    viewCoordinator.applySalesHistory(owner, data.page().items(), data.page().totalElements(),
                            data.summary(), shBusinessUnitCombo);
                },
                error -> owner.showPosLoadError("histórico de vendas", error));
    }

    private record SalesHistoryData(
            mz.multicore.erp.architecture.paging.PageResponse<InvoiceDTO> page,
            POSSalesSummaryDTO summary) {}
}

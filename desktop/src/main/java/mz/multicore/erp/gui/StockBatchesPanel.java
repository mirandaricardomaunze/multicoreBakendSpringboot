package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Lotes, validades, filtros e exportação. */
final class StockBatchesPanel {
    private final StockPanel owner;
    private final StockBatchesViewCoordinator coordinator = new StockBatchesViewCoordinator();

    StockBatchesPanel(StockPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernButton addBatchBtn = UIHelper.createSuccessButton("Adicionar Lote/Validade");
        addBatchBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBatchBtn.addActionListener(e -> owner.createBatchEntryDialog(null));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> coordinator.exportBatchesPdf(owner));

        owner.batchWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.batchWarehouseCombo);
        owner.batchWarehouseCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        owner.batchWarehouseCombo.addActionListener(e -> coordinator.filterBatches(owner));

        owner.batchExpirationCombo = new JComboBox<>(new String[]{
                "Todos os lotes",
                "Vencidos",
                "Vence em ≤ 30 dias",
                "Vence em ≤ 90 dias",
                "Válidos (> 90 dias)"
        });
        UIHelper.styleComboBox(owner.batchExpirationCombo);
        owner.batchExpirationCombo.setPreferredSize(new Dimension(145, UIHelper.FORM_CONTROL_HEIGHT));
        owner.batchExpirationCombo.addActionListener(e -> coordinator.filterBatches(owner));

        owner.batchBusinessUnitCombo = new JComboBox<>();
        owner.batchBusinessUnitCombo.addItem("Todos os negócios");
        UIHelper.styleComboBox(owner.batchBusinessUnitCombo);
        owner.batchBusinessUnitCombo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));
        owner.batchBusinessUnitCombo.addActionListener(e -> coordinator.filterBatches(owner));

        owner.batchSearchField = new SearchField("Pesquisar por SKU, nome ou lote…");
        owner.batchSearchField.setPreferredSize(new Dimension(175, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.onTextChange(owner.batchSearchField, () -> coordinator.filterBatches(owner));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes do lote (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar lote");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(
                owner.batchSearchField,
                TableFilter.label("Negócio:"), owner.batchBusinessUnitCombo,
                TableFilter.label("Armazém:"), owner.batchWarehouseCombo,
                TableFilter.label("Validade:"), owner.batchExpirationCombo,
                peekToggleBtn
        );

        owner.batchesSummary = new JLabel(" ");
        owner.batchesSummary.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        owner.batchesSummary.setForeground(UIHelper.TEXT_MUTED);
        owner.batchesSummary.setBorder(new EmptyBorder(6, 2, 0, 0));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel filterCardHeader = new JPanel(new BorderLayout(0, 6));
        filterCardHeader.setOpaque(false);
        filterCardHeader.setBorder(new EmptyBorder(0, 0, 4, 0));
        filterCardHeader.add(filters, BorderLayout.CENTER);
        filterCardHeader.add(owner.batchesSummary, BorderLayout.SOUTH);
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        card.add(UIHelper.tableCardTop("Lotes & Validades", filterCardHeader,
                refreshBtn, exportBtn, addBatchBtn), BorderLayout.NORTH);

        String[] cols = {"Artigo (SKU)", "Nome do Artigo", "Armazém", "Nº Lote", "Validade", "Dias", "Quantidade", "Estado", "Negócio"};
        owner.batchesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.batchesTable = new JTable(owner.batchesModel);
        UIHelper.styleTable(owner.batchesTable);
        owner.batchesTable.setAutoCreateRowSorter(true);
        UIHelper.ensureHeadersFit(owner.batchesTable);
        TableQuickPeekController peekController = TableQuickPeekController.install(owner.batchesTable, card,
                (peek, mr) -> coordinator.populateQuickPeek(peek, mr, owner));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane scroll = new JScrollPane(owner.batchesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(owner.batchesTable), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.batchesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.findBatchesByCompany(companyId),
                loaded -> coordinator.applyBatches(owner, loaded),
                error -> owner.showStockLoadError("lotes", error));
    }
}

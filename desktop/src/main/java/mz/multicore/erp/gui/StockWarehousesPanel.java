package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Vista e operações de gestão de armazéns. */
final class StockWarehousesPanel {
    private final StockPanel owner;

    private final JLabel kpiActiveWarehouses = new JLabel("0");
    private final JLabel kpiActiveSubtitle = new JLabel("0 loja(s) POS · 0 depósito(s)");
    private final JLabel kpiTotalCapacity = new JLabel("—");
    private final JLabel kpiTotalStockValue = new JLabel("0,00 MT");
    private final JLabel kpiDistributionNetwork = new JLabel("0 Tipos");

    StockWarehousesPanel(StockPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiGrid.add(KpiCard.createMetricCard("Armazéns Activos", kpiActiveWarehouses,
                kpiActiveSubtitle, "fas-warehouse", UIHelper.ACCENT_CYAN, null));
        kpiGrid.add(KpiCard.createCard("Capacidade Instalada", kpiTotalCapacity,
                "Capacidade máxima de armazenagem", "fas-boxes", UIHelper.MODULE_STOCK));
        kpiGrid.add(KpiCard.createCard("Valor em Armazém", kpiTotalStockValue,
                "Existências valorizadas em stock", "fas-coins", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Rede de Distribuição", kpiDistributionNetwork,
                "Lojas, depósitos e centros de distribuição", "fas-network-wired", UIHelper.ACCENT_BLUE));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Armazém");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> warehouseDialog(null));
        ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> { WarehouseDTO w = selectedManagedWarehouse(); if (w != null) warehouseDialog(w); });
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedWarehouse);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        String[] cols = {"Nome", "Nº", "Tipo", "Capacidade", "Localização", "Responsável", "Telefone", "Vendas", "Estado"};
        owner.warehousesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.warehousesTable = new JTable(owner.warehousesModel);
        UIHelper.styleTable(owner.warehousesTable);
        owner.warehousesTable.putClientProperty("noRowInspector", Boolean.TRUE);
        owner.warehousesTable.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.status());
        owner.warehousesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { WarehouseDTO w = selectedManagedWarehouse(); if (w != null) warehouseDialog(w); }
            }
        });
        JScrollPane scroll = new JScrollPane(owner.warehousesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField whSearch = TableFilter.searchField("Nome, nº, localização ou responsável…");
        JComboBox<String> whTipo = TableFilter.combo("Todos os tipos",
                "Loja", "Depósito", "Armazém Central", "Trânsito");
        UIHelper.styleComboBox(whTipo);
        whTipo.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> whEstado = TableFilter.combo("Todos os estados", "Activo", "Inactivo");
        UIHelper.styleComboBox(whEstado);
        whEstado.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(owner.warehousesTable, whSearch,
                new TableFilter.ColumnFilter(whTipo, 2),
                new TableFilter.ColumnFilter(whEstado, 8));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes do armazém (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar armazém");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel whFilters = TableFilter.bar(
                whSearch,
                TableFilter.label("Tipo:"), whTipo,
                TableFilter.label("Estado:"), whEstado,
                peekToggleBtn
        );
        whFilters.setBorder(new EmptyBorder(0, 0, 4, 0));
        card.add(UIHelper.tableCardTop("Gestão de Armazéns", whFilters,
                refreshBtn, moreBtn, editBtn, newBtn), BorderLayout.NORTH);
        UIHelper.ensureHeadersFit(owner.warehousesTable);
        TableQuickPeekController peekController = TableQuickPeekController.install(owner.warehousesTable, card,
                (peek, mr) -> StockWarehouseViewCoordinator.populatePeek(peek, owner.warehousesModel, mr, () -> {
                    WarehouseDTO w = selectedManagedWarehouse();
                    if (w != null) warehouseDialog(w);
                }));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(owner.warehousesTable), BorderLayout.SOUTH);
        tab.add(kpiGrid, BorderLayout.NORTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        if (owner.warehousesModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getAllWarehousesByCompany(companyId),
                this::applyWarehousesManagement, error -> owner.showStockLoadError("gestão de armazéns", error));
    }

    private void applyWarehousesManagement(List<WarehouseDTO> loaded) {
        owner.warehousesFullList = loaded;
        owner.warehousesModel.setRowCount(0);
        for (WarehouseDTO w : owner.warehousesFullList) {
            owner.warehousesModel.addRow(new Object[]{
                    w.name(),
                    w.warehouseNumber() == null ? "—" : w.warehouseNumber(),
                    w.type() == null ? "—" : w.type().label(),
                    w.capacity() == null ? "—" : String.format("%,.2f", w.capacity()),
                    w.location() == null ? "—" : w.location(),
                    w.manager() == null ? "—" : w.manager(),
                    w.phone() == null ? "—" : w.phone(),
                    w.allowsSales() ? "Sim" : "Não",
                    w.active() ? "ACTIVO" : "INATIVO"});
        }
        updateKpis();
    }

    public void updateKpis() {
        StockWarehouseViewCoordinator.updateKpis(owner.warehousesFullList, owner.stocksList,
                kpiActiveWarehouses, kpiActiveSubtitle, kpiTotalCapacity, kpiTotalStockValue, kpiDistributionNetwork);
    }

    private WarehouseDTO selectedManagedWarehouse() {
        int row = owner.warehousesTable == null ? -1 : owner.warehousesTable.getSelectedRow();
        if (row < 0 || owner.warehousesFullList == null || row >= owner.warehousesFullList.size()) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione um armazém", "Escolha um armazém na tabela para continuar.");
            return null;
        }
        return owner.warehousesFullList.get(owner.warehousesTable.convertRowIndexToModel(row));
    }

    private void toggleSelectedWarehouse() {
        WarehouseDTO w = selectedManagedWarehouse();
        if (w == null) return;
        UIHelper.runWithProgress(owner, "A actualizar armazém…",
                () -> owner.inventoryApiClient.setWarehouseActive(w.id(), !w.active()),
                ignored -> owner.onPanelSelected(), owner::showStockError);
    }

    public void createWarehouseDialogV2() {
        warehouseDialog(null);
    }

    private void warehouseDialog(WarehouseDTO existing) {
        WarehouseFormDialog.show(owner, existing);
    }
}

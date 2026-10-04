package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/** Vista e operações de gestão de armazéns. */
final class StockWarehousesPanel {
    private final StockPanel owner;
    StockWarehousesPanel(StockPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(12, 5, 5, 5));

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
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
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

        JComboBox<String> whEstado = TableFilter.combo("Todos os estados", "ACTIVO", "INATIVO");
        UIHelper.styleComboBox(whEstado);
        whEstado.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(owner.warehousesTable, whSearch,
                new TableFilter.ColumnFilter(whTipo, 2),
                new TableFilter.ColumnFilter(whEstado, 8));

        JPanel whFilters = new JPanel(new GridBagLayout());
        whFilters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        g.gridx = 0; g.weightx = 0; whFilters.add(filterLabel("Tipo de Armazém"), g);
        g.gridx = 1; g.weightx = 0; whFilters.add(filterLabel("Estado"), g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        whFilters.add(filterLabel("Pesquisa"), g);

        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; whFilters.add(whTipo, g);
        g.gridx = 1; g.weightx = 0; whFilters.add(whEstado, g);
        g.gridx = 2; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        whFilters.add(whSearch, g);

        whFilters.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(UIHelper.tableCardTop("Gestão de Armazéns", whFilters,
                refreshBtn, moreBtn, editBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(owner.warehousesTable), BorderLayout.SOUTH);
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
    }

    private WarehouseDTO selectedManagedWarehouse() {
        int row = owner.warehousesTable == null ? -1 : owner.warehousesTable.getSelectedRow();
        if (row < 0 || row >= owner.warehousesFullList.size()) {
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

    /** Diálogo de criar/editar armazém. {@code existing == null} → criar; senão → editar. */
    private void warehouseDialog(WarehouseDTO existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.name() : "");
        JTextField numberField = new JTextField(editing && existing.warehouseNumber() != null ? existing.warehouseNumber() : "");
        JTextField capacityField = new JTextField(editing && existing.capacity() != null ? existing.capacity().toPlainString() : "0");
        JTextField locField = new JTextField(editing && existing.location() != null ? existing.location() : "");
        JComboBox<String> typeCombo = new JComboBox<>();
        for (var t : mz.multicore.erp.modules.inventory.model.WarehouseType.values()) typeCombo.addItem(t.label());
        UIHelper.styleComboBox(typeCombo);
        if (editing && existing.type() != null) typeCombo.setSelectedIndex(existing.type().ordinal());
        JTextField managerField = new JTextField(editing && existing.manager() != null ? existing.manager() : "");
        JTextField phoneField = new JTextField(editing && existing.phone() != null ? existing.phone() : "");
        UIHelper.styleTextField(managerField);
        UIHelper.styleTextField(phoneField);
        JCheckBox allowsSalesCheck = new JCheckBox("Permite vendas ao balcão (POS)", editing ? existing.allowsSales() : true);
        allowsSalesCheck.setOpaque(false);
        allowsSalesCheck.setForeground(UIHelper.TEXT_LIGHT);

        JPanel dialogPanel = UIHelper.createDialogForm(
                "Nome do Armazem:", nameField,
                "Numero do Armazem:", numberField,
                "Tipo:", typeCombo,
                "Capacidade:", capacityField,
                "Localizacao / Endereco:", locField,
                "Responsável:", managerField,
                "Telefone:", phoneField,
                "Vendas:", allowsSalesCheck
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                editing ? "Editar Armazém" : "Criar Novo Armazém", "fas-warehouse",
                editing ? "Actualize os dados do local" : "Registe um novo local de stock", dialogPanel).showDialog();
        if (confirmed) {
            String name = nameField.getText().trim();
            String warehouseNumber = numberField.getText().trim();
            String capacityStr = capacityField.getText().trim();
            String location = locField.getText().trim();

            if (name.isEmpty() || warehouseNumber.isEmpty()) {
                owner.showStockNotice(FeedbackType.ERROR, "Campos obrigatórios", "Nome e número do armazém são obrigatórios.");
                return;
            }

            try {
                BigDecimal capacity = capacityStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(capacityStr);
                if (capacity.compareTo(BigDecimal.ZERO) < 0) {
                    owner.showStockNotice(FeedbackType.ERROR, "Capacidade inválida", "A capacidade deve ser zero ou superior.");
                    return;
                }
                mz.multicore.erp.modules.inventory.model.WarehouseType type =
                        mz.multicore.erp.modules.inventory.model.WarehouseType.values()[Math.max(0, typeCombo.getSelectedIndex())];

                boolean allowsSales = allowsSalesCheck.isSelected();
                String manager = managerField.getText().trim();
                String phone = phoneField.getText().trim();
                Long companyId = CurrentUserContext.getCurrentCompanyId();
                UIHelper.runWithProgress(owner, "A guardar armazém…", () -> {
                    if (editing) {
                        return owner.inventoryApiClient.updateWarehouse(existing.id(), new UpdateWarehouseRequest(
                                name, warehouseNumber, capacity, location, type, allowsSales, manager, phone));
                    }
                    return owner.inventoryApiClient.createWarehouse(new CreateWarehouseRequest(
                            name, warehouseNumber, capacity, location, companyId, type, allowsSales, manager, phone));
                }, ignored -> {
                    owner.showStockSuccess(editing ? "Armazém '" + name + "' actualizado."
                            : "Armazém '" + name + "' criado com sucesso.");
                    owner.onPanelSelected();
                }, owner::showStockError);
            } catch (NumberFormatException ex) {
                owner.showStockNotice(FeedbackType.ERROR, "Capacidade inválida", "A capacidade deve ser um valor numérico.");
            } catch (Exception ex) {
                owner.showStockNotice(FeedbackType.ERROR, "Não foi possível gravar o armazém", ex.getMessage());
            }
        }
    }

    /**
     * Inventário físico (contagem cega): imprime a folha de contagem, permite introduzir as contagens
     * por artigo de um armazém e reconcilia — cada artigo contado gera um ajuste de stock (define a
     * quantidade contada) e mostra a diferença face ao sistema. Artigos deixados em branco não são
     * tocados (só se ajusta o que foi efectivamente contado).
     */
    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}

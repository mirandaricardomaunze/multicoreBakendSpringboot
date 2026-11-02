package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Listagem e manutenção de fornecedores. */
final class PurchaseSuppliersPanel {
    private final ComprasPanel owner;
    private JTextField supplierSearchField;

    PurchaseSuppliersPanel(ComprasPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Gerir Fornecedor")
                .addAction("Editar", UIHelper.icon("fas-edit", 14), () -> {
                    SupplierDTO sel = PurchaseSuppliersViewCoordinator.selectedSupplier(owner);
                    if (sel != null) openSupplierDialog(sel);
                })
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14),
                        () -> PurchaseSuppliersViewCoordinator.toggleSelectedSupplier(owner));

        ModernButton refreshSupsBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshSupsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshSupsBtn.addActionListener(e -> { supplierSearchField.setText(""); owner.loadSuppliers(); });

        ModernButton newSupBtn = UIHelper.createSuccessButton("Novo Fornecedor");
        newSupBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newSupBtn.addActionListener(e -> openSupplierDialog(null));

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 6));
        listCard.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[] supCols = {"Nome do Fornecedor", "NUIT/NIF", "Telefone", "Contacto", "Correio Eletrónico", "Endereço", "Estado"};
        owner.suppliersModel = new DefaultTableModel(supCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.suppliersTable = new JTable(owner.suppliersModel);
        UIHelper.styleTable(owner.suppliersTable);
        owner.suppliersTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        UIHelper.ensureHeadersFit(owner.suppliersTable);
        JScrollPane scroll = new JScrollPane(owner.suppliersTable);
        UIHelper.styleScrollPane(scroll);

        supplierSearchField = TableFilter.searchField("Nome ou NUIT…");
        JComboBox<String> supEstado = TableFilter.combo("Todos os estados", "Activo", "Inactivo");

        TableFilter.install(owner.suppliersTable, supplierSearchField,
                new TableFilter.ColumnFilter(supEstado, 6));

        JPanel supFilters = TableFilter.bar(
                supplierSearchField,
                TableFilter.label("Estado:"), supEstado);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes do fornecedor (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(supFilters, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        listCard.add(UIHelper.tableCardTop("Fornecedores Cadastrados", filtersRow,
                refreshSupsBtn, actionsMenu, newSupBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        listCard.add(ClientTablePagination.install(owner.suppliersTable), BorderLayout.SOUTH);

        TableQuickPeekController peek = TableQuickPeekController.install(owner.suppliersTable, listCard,
                (p, mr) -> PurchaseSuppliersViewCoordinator.populatePeek(p, mr, owner, this::openSupplierDialog));
        peekToggleBtn.addActionListener(e -> peek.toggle());

        panel.add(listCard, BorderLayout.CENTER);
        return panel;
    }

    private void openSupplierDialog(SupplierDTO existing) {
        SupplierFormDialog.show(owner, existing);
    }
}

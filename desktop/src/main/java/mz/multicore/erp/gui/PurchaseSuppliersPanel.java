package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
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
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Ações")
                .addAction("Editar", UIHelper.icon("fas-edit", 14), () -> {
                    SupplierDTO sel = selectedSupplier();
                    if (sel != null) openSupplierDialog(sel);
                })
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedSupplier);

        ModernButton refreshSupsBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshSupsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshSupsBtn.addActionListener(e -> { supplierSearchField.setText(""); owner.loadSuppliers(); });

        ModernButton newSupBtn = UIHelper.createSuccessButton("Novo Fornecedor");
        newSupBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newSupBtn.addActionListener(e -> openSupplierDialog(null));

        // Table full-width
        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 10));
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] supCols = {"Nome do Fornecedor", "NUIT/NIF", "Telefone", "Contacto", "Correio Eletrónico", "Endereço", "Estado"};
        owner.suppliersModel = new DefaultTableModel(supCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.suppliersTable = new JTable(owner.suppliersModel);
        UIHelper.styleTable(owner.suppliersTable);
        owner.suppliersTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(owner.suppliersTable);
        UIHelper.styleScrollPane(scroll);

        supplierSearchField = TableFilter.searchField("Nome ou NUIT…");
        JComboBox<String> supEstado = TableFilter.combo("Todos os estados", "Activo", "Inactivo");
        UIHelper.styleComboBox(supEstado);
        supEstado.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(owner.suppliersTable, supplierSearchField,
                new TableFilter.ColumnFilter(supEstado, 6));

        JPanel supFilters = new JPanel(new GridBagLayout());
        supFilters.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);

        g.gridx = 0; g.weightx = 0; supFilters.add(filterLabel("Estado"), g);
        g.gridx = 1; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0);
        supFilters.add(filterLabel("Pesquisa"), g);

        g.gridy = 1;
        g.insets = new Insets(4, 0, 0, 12);
        g.gridx = 0; g.weightx = 0; supFilters.add(supEstado, g);
        g.gridx = 1; g.weightx = 1.0; g.insets = new Insets(4, 0, 0, 0);
        supFilters.add(supplierSearchField, g);

        supFilters.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Fornecedores Cadastrados", supFilters,
                refreshSupsBtn, actionsMenu, newSupBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        listCard.add(ClientTablePagination.install(owner.suppliersTable), BorderLayout.SOUTH);
        panel.add(listCard, BorderLayout.CENTER);
        return panel;
    }

    private SupplierDTO selectedSupplier() {
        int row = TableFilter.selectedModelRow(owner.suppliersTable);
        if (row < 0 || row >= owner.suppliersList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione um fornecedor", "Escolha um fornecedor na tabela para continuar.");
            return null;
        }
        return owner.suppliersList.get(row);
    }

    private void toggleSelectedSupplier() {
        SupplierDTO sel = selectedSupplier();
        if (sel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A actualizar fornecedor…",
                () -> owner.purchaseApiClient.setSupplierActive(sel.id(), companyId, !sel.active()),
                ignored -> owner.loadSuppliers(), owner::showPurchaseError);
    }

    private void openSupplierDialog(SupplierDTO existing) {
        boolean editing = existing != null;
        JTextField nameField = new JTextField(editing ? existing.name() : "");
        JTextField taxIdField = new JTextField(editing ? existing.taxId() : "");
        JTextField phoneField = new JTextField(editing && existing.phone() != null ? existing.phone() : "");
        JTextField contactField = new JTextField(editing && existing.contactPerson() != null ? existing.contactPerson() : "");
        JTextField emailField = new JTextField(editing && existing.email() != null ? existing.email() : "");
        JTextField addressField = new JTextField(editing && existing.address() != null ? existing.address() : "");

        JPanel form = UIHelper.createDialogForm(
                "Nome / Empresa:", nameField,
                "NUIT / NIF (9 dígitos):", taxIdField,
                "Telefone:", phoneField,
                "Pessoa de Contacto:", contactField,
                "Correio Eletrónico:", emailField,
                "Endereço:", addressField
        );

        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent, editing ? "Editar Fornecedor" : "Novo Fornecedor", form);
        dlg.setSize(520, 480);
        dlg.setOnSaveAsync(() -> {
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            if (name.isEmpty() || taxId.isEmpty()) {
                throw new RuntimeException("Nome e NUIT/NIF são campos obrigatórios.");
            }
            mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest req =
                    new mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest(
                            name, taxId,
                            emailField.getText().trim(),
                            addressField.getText().trim(),
                            phoneField.getText().trim(),
                            contactField.getText().trim(),
                            CurrentUserContext.getCurrentCompanyId());
            return () -> editing ? owner.purchaseApiClient.updateSupplier(existing.id(), req)
                    : owner.purchaseApiClient.createSupplier(req);
        });

        if (dlg.showDialog()) {
            owner.showPurchaseSuccess("Fornecedor '" + nameField.getText().trim()
                    + (editing ? "' actualizado." : "' registado."));
            owner.loadSuppliers();
        }
    }

    private JLabel filterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }
}

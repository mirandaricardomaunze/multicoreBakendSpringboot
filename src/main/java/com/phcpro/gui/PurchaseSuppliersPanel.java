package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.purchases.dto.SupplierDTO;

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

        // Header: title + action buttons
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Fornecedores Cadastrados"), BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        ModernButton editSupBtn = UIHelper.createSecondaryButton("Editar");
        editSupBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton toggleSupBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleSupBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        ModernButton refreshSupsBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshSupsBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        ModernButton newSupBtn = UIHelper.createSuccessButton("Novo Fornecedor");
        newSupBtn.setIcon(UIHelper.icon("fas-plus", 14));
        headerActions.add(editSupBtn);
        headerActions.add(toggleSupBtn);
        headerActions.add(refreshSupsBtn);
        headerActions.add(newSupBtn);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        TableFilter.install(owner.suppliersTable, supplierSearchField,
                new TableFilter.ColumnFilter(supEstado, 6));
        JPanel supBar = TableFilter.bar(supplierSearchField,
                TableFilter.label("Estado:"), supEstado);
        supBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(supBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        refreshSupsBtn.addActionListener(e -> { supplierSearchField.setText(""); owner.loadSuppliers(); });
        newSupBtn.addActionListener(e -> openSupplierDialog(null));
        editSupBtn.addActionListener(e -> {
            SupplierDTO sel = selectedSupplier();
            if (sel != null) openSupplierDialog(sel);
        });
        toggleSupBtn.addActionListener(e -> toggleSelectedSupplier());

        return panel;
    }

    private SupplierDTO selectedSupplier() {
        int row = TableFilter.selectedModelRow(owner.suppliersTable);
        if (row < 0 || row >= owner.suppliersList.size()) {
            JOptionPane.showMessageDialog(owner, "Selecione um fornecedor na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
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
            com.phcpro.modules.purchases.dto.CreateSupplierRequest req =
                    new com.phcpro.modules.purchases.dto.CreateSupplierRequest(
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
            JOptionPane.showMessageDialog(owner,
                    "Fornecedor '" + nameField.getText().trim() + (editing ? "' actualizado." : "' registado."),
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            owner.loadSuppliers();
        }
    }

}
